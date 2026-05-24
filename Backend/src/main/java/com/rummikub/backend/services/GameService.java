package com.rummikub.backend.services;

import com.rummikub.backend.models.Game;
import com.rummikub.backend.models.GameParticipant;
import com.rummikub.backend.models.GameTile;
import com.rummikub.backend.models.Tile;
import com.rummikub.backend.models.User;
import com.rummikub.backend.models.enums.GameStatus;
import com.rummikub.backend.models.enums.TileLocation;
import com.rummikub.backend.repositories.GameParticipantRepository;
import com.rummikub.backend.repositories.GameRepository;
import com.rummikub.backend.repositories.GameTileRepository;
import com.rummikub.backend.repositories.UserRepository;
import com.rummikub.backend.models.TableSet;
import com.rummikub.backend.repositories.TableSetRepository;
import com.rummikub.backend.repositories.TileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    @Autowired private GameRepository gameRepository;
    @Autowired private GameParticipantRepository participantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GameTileRepository gameTileRepository;
    @Autowired private TableSetRepository tableSetRepository;
    @Autowired private TileRepository tileRepository;
    @Autowired private RummikubLogicService rummikubLogicService;
    @Autowired private TurnValidatorService turnValidatorService;

    // Heartbeat tracking in-memory
    private record PingData(String gameId, Instant lastSeen) {}
    private final Map<String, PingData> activeHeartbeats = new ConcurrentHashMap<>();

    public void recordHeartbeat(String gameId, String userId) {
        activeHeartbeats.put(userId, new PingData(gameId, Instant.now()));
    }

    @Scheduled(fixedRate = 5000)
    public void sweepDisconnectedPlayers() {
        Instant cutoff = Instant.now().minusSeconds(15);
        for (Map.Entry<String, PingData> entry : activeHeartbeats.entrySet()) {
            if (entry.getValue().lastSeen().isBefore(cutoff)) {
                String timedOutUserId = entry.getKey();
                String gameId = entry.getValue().gameId();
                activeHeartbeats.remove(timedOutUserId);
                try {
                    leaveGame(gameId, UUID.fromString(timedOutUserId));
                } catch (Exception e) {
                    // Log and ignore if the game is already over or invalid
                }
            }
        }
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional
    public Game createGame() {
        Game game = new Game();
        String newId;
        do {
            newId = generateRoomCode();
        } while (gameRepository.existsById(newId));
        game.setId(newId);
        return gameRepository.save(game);
    }

    @Transactional
    public GameParticipant joinGame(String gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game tidak ditemukan."));
        if (game.getStatus() != GameStatus.WAITING) {
            throw new RuntimeException("Game sudah dimulai atau selesai.");
        }

        int participantCount = participantRepository.countByGameId(gameId);
        if (participantCount >= 4) {
            throw new RuntimeException("Ruangan sudah penuh (Maks 4 pemain).");
        }

        if (participantRepository.findByGameIdAndUserId(gameId, userId).isPresent()) {
            throw new RuntimeException("Anda sudah bergabung dalam permainan ini.");
        }

        User user = userRepository.findById(userId).orElseThrow();
        GameParticipant participant = new GameParticipant();
        participant.setGame(game);
        participant.setUser(user);
        participant.setTurnOrder((short) (participantCount + 1));
        return participantRepository.save(participant);
    }

    @Transactional
    public Map<String, Object> startGame(String gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow();
        if (game.getStatus() != GameStatus.WAITING) {
            throw new RuntimeException("Game sudah berjalan.");
        }

        List<GameParticipant> participants = participantRepository.findByGameIdOrderByTurnOrderAsc(gameId);
        if (participants.size() < 2) {
            throw new RuntimeException("Minimal 2 pemain untuk memulai.");
        }

        GameParticipant firstParticipant = participants.get(0);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentTurnParticipant(firstParticipant);
        game.setTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);

        // Setup 106 Ubin ke dalam Pool
        List<Tile> masterTiles = tileRepository.findAll();
        if (masterTiles.isEmpty()) {
            throw new RuntimeException("Master tiles (106 ubin) belum ada di database. Silakan jalankan script seeder/dump.");
        }

        List<GameTile> poolTiles = new ArrayList<>();
        for (Tile t : masterTiles) {
            GameTile gt = new GameTile();
            gt.setGame(game);
            gt.setTile(t);
            gt.setLocation(TileLocation.POOL);
            poolTiles.add(gt);
        }
        gameTileRepository.saveAll(poolTiles);
        gameTileRepository.flush();

        // Bagi 14 ubin ke masing-masing pemain
        List<GameTile> allPoolTiles = gameTileRepository.findByGameIdAndLocationStr(gameId, TileLocation.POOL.name());
        Collections.shuffle(allPoolTiles);
        
        List<GameTile> tilesToUpdate = new ArrayList<>();
        int tileIndex = 0;
        for (GameParticipant p : participants) {
            for (int i = 0; i < 14; i++) {
                if (tileIndex < allPoolTiles.size()) {
                    GameTile gt = allPoolTiles.get(tileIndex++);
                    gt.setLocation(TileLocation.RACK);
                    gt.setParticipant(p);
                    tilesToUpdate.add(gt);
                }
            }
        }
        gameTileRepository.saveAll(tilesToUpdate);

        return Map.of("message", "Game berhasil dimulai!", "firstTurnId", firstParticipant.getId());
    }

    private void switchToNextTurn(Game game) {
        List<GameParticipant> participants = participantRepository.findByGameIdOrderByTurnOrderAsc(game.getId());
        int currIdx = -1;
        for (int i = 0; i < participants.size(); i++) {
            if (participants.get(i).getId().equals(game.getCurrentTurnParticipant().getId())) {
                currIdx = i;
                break;
            }
        }
        if (currIdx == -1) currIdx = 0;
        
        int nextIdx = (currIdx + 1) % participants.size();
        
        int loopCount = 0;
        while (participants.get(nextIdx).isHasLeft() && loopCount < participants.size()) {
            nextIdx = (nextIdx + 1) % participants.size();
            loopCount++;
        }

        game.setCurrentTurnParticipant(participants.get(nextIdx));
        game.setTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);
    }

    @Transactional
    public Map<String, Object> executeEndTurn(String gameId, UUID userId, List<RummikubLogicService.TableSetRequest> tableSets, List<Integer> rackTileIds) {
        GameParticipant participant = participantRepository.findByGameIdAndUserId(gameId, userId).orElseThrow(() -> new RuntimeException("Anda bukan peserta game ini."));
        Game game = gameRepository.findById(gameId).orElseThrow();

        if (game.getStatus() != GameStatus.IN_PROGRESS) throw new RuntimeException("Game tidak sedang berlangsung.");
        if (!game.getCurrentTurnParticipant().getId().equals(participant.getId())) throw new RuntimeException("Bukan giliran Anda.");

        long secondsElapsed = ChronoUnit.SECONDS.between(game.getTurnStartedAt(), LocalDateTime.now());
        if (secondsElapsed > 125) {
            throw new RuntimeException("Waktu giliran Anda sudah habis. Silakan endpoint draw/penalty!");
        }

        TurnValidatorService.EndTurnResult result = turnValidatorService.executeEndTurn(gameId, participant.getId(), tableSets, rackTileIds);
        
        if (result.isFinished) {
            // Resolve the winner's username from the userId
            String winnerUsername = userRepository.findById(result.winnerId)
                .map(u -> u.getUsername())
                .orElse("Unknown");
            return Map.of("message", "Game Selesai! Anda menang.", "winner", winnerUsername, "gameOver", true);
        }

        switchToNextTurn(game);
        return Map.of("message", "Giliran berhasil diselesaikan.", "nextTurnId", game.getCurrentTurnParticipant().getId());
    }

    @Transactional
    public Map<String, Object> drawTilePenalty(String gameId, UUID userId) {
        GameParticipant participant = participantRepository.findByGameIdAndUserId(gameId, userId).orElseThrow();
        Game game = gameRepository.findById(gameId).orElseThrow();

        if (!game.getCurrentTurnParticipant().getId().equals(participant.getId())) throw new RuntimeException("Bukan giliran Anda.");

        List<GameTile> poolTiles = gameTileRepository.findByGameIdAndLocationStr(gameId, TileLocation.POOL.name());
        if (poolTiles.isEmpty()) {
            throw new RuntimeException("Pool sudah habis, tapi giliran diputar.");
        }

        Collections.shuffle(poolTiles);
        GameTile drawnTile = poolTiles.get(0);
        drawnTile.setLocation(TileLocation.RACK);
        drawnTile.setParticipant(participant);
        gameTileRepository.save(drawnTile);

        switchToNextTurn(game);
        return Map.of("message", "Ubin diambil. Giliran berpindah.", "nextTurnId", game.getCurrentTurnParticipant().getId(), "drawnTileId", drawnTile.getTile().getId());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGameState(String gameId, UUID userId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new RuntimeException("Game tidak ditemukan."));
        List<GameParticipant> participants = participantRepository.findByGameIdOrderByTurnOrderAsc(gameId);

        List<Map<String, Object>> participantDtos = new ArrayList<>();
        for (GameParticipant p : participants) {
            Map<String, Object> pdto = new HashMap<>();
            pdto.put("userId", p.getUser().getId().toString());
            pdto.put("username", p.getUser().getUsername());
            pdto.put("turnOrder", p.getTurnOrder());
            pdto.put("score", p.getScore());
            pdto.put("hasLeft", p.isHasLeft());
            pdto.put("hasDoneInitialMeld", p.isHasDoneInitialMeld());
            participantDtos.add(pdto);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", game.getId().toString());
        data.put("status", game.getStatus().toString());
        data.put("currentTurnUserId", game.getCurrentTurnParticipant() != null ? game.getCurrentTurnParticipant().getUser().getId().toString() : null);
        data.put("turnStartedAt", game.getTurnStartedAt() != null 
            ? game.getTurnStartedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() 
            : null);
        data.put("participants", participantDtos);
        // Resolve winner if game is finished
        String winnerUsername = null;
        if (game.getStatus() == GameStatus.FINISHED && game.getWinnerParticipant() != null) {
            winnerUsername = game.getWinnerParticipant().getUser().getUsername();
        }
        data.put("winner", winnerUsername);

        Optional<GameParticipant> requesterOpt = participantRepository.findByGameIdAndUserId(gameId, userId);
        if (requesterOpt.isPresent()) {
            GameParticipant requester = requesterOpt.get();
            data.put("hasDoneInitialMeld", requester.isHasDoneInitialMeld());
            data.put("meldScore", requester.getScore());

            List<GameTile> rackTiles = gameTileRepository.findByGameIdAndParticipantIdAndLocationStr(gameId, requester.getId(), TileLocation.RACK.name());
            List<Map<String, Object>> rackDtos = new ArrayList<>();
            for (GameTile gt : rackTiles) {
                rackDtos.add(Map.of(
                    "id", gt.getTile().getId(),
                    "number", gt.getTile().getNumber() != null ? gt.getTile().getNumber() : 0,
                    "color", gt.getTile().getColor() != null ? gt.getTile().getColor().toString() : "NONE",
                    "isJoker", gt.getTile().isJoker()
                ));
            }
            data.put("myRackTiles", rackDtos);
        }

        List<TableSet> sets = tableSetRepository.findByGameId(gameId);
        List<GameTile> tableTiles = gameTileRepository.findByGameIdAndLocationStr(gameId, TileLocation.TABLE.name());
        
        Map<UUID, List<Integer>> tilesBySet = new HashMap<>();
        for (GameTile gt : tableTiles) {
            if (gt.getTableSet() != null) {
                tilesBySet.computeIfAbsent(gt.getTableSet().getId(), k -> new ArrayList<>()).add(gt.getTile().getId());
            }
        }

        List<Map<String, Object>> tableSetDtos = new ArrayList<>();
        for (TableSet ts : sets) {
            List<Integer> setTileIds = tilesBySet.getOrDefault(ts.getId(), new ArrayList<>());
            Map<String, Object> setDto = new HashMap<>();
            setDto.put("set_type", ts.getSetType().toString());
            setDto.put("tile_ids", setTileIds);
            setDto.put("tiles", buildTileDetails(setTileIds));
            tableSetDtos.add(setDto);
        }
        data.put("tableSets", tableSetDtos);

        return data;
    }

    /**
     * Builds a list of tile detail maps for the given tile IDs.
     * This allows the frontend to cache tile metadata (color, number, isJoker)
     * for set type detection without a separate API call.
     */
    private List<Map<String, Object>> buildTileDetails(List<Integer> tileIds) {
        List<Map<String, Object>> details = new ArrayList<>();
        for (Integer tileId : tileIds) {
            Tile tile = rummikubLogicService.getTile(tileId);
            if (tile != null) {
                details.add(Map.of(
                    "id", tile.getId(),
                    "number", tile.getNumber() != null ? tile.getNumber() : 0,
                    "color", tile.getColor() != null ? tile.getColor().toString() : "NONE",
                    "isJoker", tile.isJoker()
                ));
            }
        }
        return details;
    }

    @Transactional
    public Map<String, Object> applyCheat(String gameId, UUID userId) {
        GameParticipant participant = participantRepository.findByGameIdAndUserId(gameId, userId)
                .orElseThrow(() -> new RuntimeException("Anda bukan peserta game ini."));
        Game game = gameRepository.findById(gameId).orElseThrow();

        if (game.getStatus() != GameStatus.IN_PROGRESS) throw new RuntimeException("Game tidak sedang berlangsung.");
        if (!game.getCurrentTurnParticipant().getId().equals(participant.getId())) throw new RuntimeException("Bukan giliran Anda.");

        List<GameTile> allGameTiles = gameTileRepository.findByGameId(gameId);
        
        List<GameTile> rackTiles = new ArrayList<>();
        List<GameTile> twoJokers = new ArrayList<>();
        GameTile highValueTile = null;

        for (GameTile gt : allGameTiles) {
            if (gt.getParticipant() != null && gt.getParticipant().getId().equals(participant.getId())) {
                rackTiles.add(gt);
            }
            if (gt.getTile().isJoker() && twoJokers.size() < 2) {
                twoJokers.add(gt);
            } else if (!gt.getTile().isJoker() && gt.getTile().getNumber() != null && gt.getTile().getNumber() >= 10) {
                highValueTile = gt;
            }
        }

        if (rackTiles.isEmpty()) {
            throw new RuntimeException("Rak kosong.");
        }

        // Return current rack to POOL
        for (GameTile gt : rackTiles) {
            gt.setLocation(TileLocation.POOL);
            gt.setParticipant(null);
        }

        // Assign 2 Jokers and 1 High Value tile to the player's rack
        List<GameTile> newRack = new ArrayList<>();
        for (GameTile joker : twoJokers) {
            joker.setLocation(TileLocation.RACK);
            joker.setParticipant(participant);
            newRack.add(joker);
        }
        
        if (highValueTile != null) {
            highValueTile.setLocation(TileLocation.RACK);
            highValueTile.setParticipant(participant);
            newRack.add(highValueTile);
        }

        // Save all changes
        gameTileRepository.saveAll(allGameTiles);

        return Map.of("message", "Cheat berhasil! Rak Anda sekarang berisi Joker.", "success", true);
    }

    @Transactional
    public Map<String, Object> leaveGame(String gameId, UUID userId) {
        // Remove from heartbeat tracking so sweeper doesn't process them twice
        activeHeartbeats.remove(userId.toString());

        GameParticipant participant = participantRepository.findByGameIdAndUserId(gameId, userId).orElseThrow(() -> new RuntimeException("Anda bukan peserta game ini."));
        Game game = gameRepository.findById(gameId).orElseThrow();

        participant.setHasLeft(true);
        participantRepository.save(participant);

        List<GameTile> rackTiles = gameTileRepository.findByGameIdAndParticipantIdAndLocationStr(gameId, participant.getId(), TileLocation.RACK.name());
        for (GameTile gt : rackTiles) {
            gt.setLocation(TileLocation.POOL);
            gt.setParticipant(null);
        }
        gameTileRepository.saveAll(rackTiles);

        if (game.getStatus() == GameStatus.IN_PROGRESS) {
            List<GameParticipant> participants = participantRepository.findByGameIdOrderByTurnOrderAsc(gameId);
            long activeCount = participants.stream().filter(p -> !p.isHasLeft()).count();

            if (activeCount <= 1) {
                GameParticipant winner = participants.stream().filter(p -> !p.isHasLeft()).findFirst().orElse(null);
                game.setStatus(GameStatus.FINISHED);
                game.setWinnerParticipant(winner);
                gameRepository.save(game);
                return Map.of("message", "Anda keluar. Game selesai.", "success", true);
            } else {
                if (game.getCurrentTurnParticipant() != null && game.getCurrentTurnParticipant().getId().equals(participant.getId())) {
                    switchToNextTurn(game);
                }
            }
        }
        return Map.of("message", "Anda berhasil keluar dari game.", "success", true);
    }
}
