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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class GameService {

    @Autowired private GameRepository gameRepository;
    @Autowired private GameParticipantRepository participantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GameTileRepository gameTileRepository;
    @Autowired private RummikubLogicService rummikubLogicService;
    @Autowired private TurnValidatorService turnValidatorService;

    @Transactional
    public Game createGame() {
        Game game = new Game();
        return gameRepository.save(game);
    }

    @Transactional
    public GameParticipant joinGame(UUID gameId, UUID userId) {
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
    public Map<String, Object> startGame(UUID gameId) {
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
        List<GameTile> poolTiles = new ArrayList<>();
        for (Tile t : rummikubLogicService.getAllTiles()) {
            GameTile gt = new GameTile();
            gt.setGame(game);
            gt.setTile(t);
            gt.setLocation(TileLocation.POOL);
            poolTiles.add(gt);
        }
        gameTileRepository.saveAll(poolTiles);

        // Bagi 14 ubin ke masing-masing pemain
        List<GameTile> allPoolTiles = gameTileRepository.findByGameIdAndLocation(gameId, TileLocation.POOL);
        Collections.shuffle(allPoolTiles);
        
        int tileIndex = 0;
        for (GameParticipant p : participants) {
            for (int i = 0; i < 14; i++) {
                if(tileIndex < allPoolTiles.size()){
                    GameTile gt = allPoolTiles.get(tileIndex++);
                    gt.setLocation(TileLocation.RACK);
                    gt.setParticipant(p);
                    gameTileRepository.save(gt);
                }
            }
        }

        return Map.of("message", "Game berhasil dimulai!", "firstTurnId", firstParticipant.getId());
    }

    private void switchToNextTurn(Game game) {
        List<GameParticipant> participants = participantRepository.findByGameIdOrderByTurnOrderAsc(game.getId());
        int currIdx = participants.indexOf(game.getCurrentTurnParticipant());
        int nextIdx = (currIdx + 1) % participants.size();
        game.setCurrentTurnParticipant(participants.get(nextIdx));
        game.setTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);
    }

    @Transactional
    public Map<String, Object> executeEndTurn(UUID gameId, UUID userId, List<RummikubLogicService.TableSetRequest> tableSets, List<Integer> rackTileIds) {
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
            return Map.of("message", "Game Selesai! Anda menang.", "winner", result.winnerId);
        }

        switchToNextTurn(game);
        return Map.of("message", "Giliran berhasil diselesaikan.", "nextTurnId", game.getCurrentTurnParticipant().getId());
    }

    @Transactional
    public Map<String, Object> drawTilePenalty(UUID gameId, UUID userId) {
        GameParticipant participant = participantRepository.findByGameIdAndUserId(gameId, userId).orElseThrow();
        Game game = gameRepository.findById(gameId).orElseThrow();

        if (!game.getCurrentTurnParticipant().getId().equals(participant.getId())) throw new RuntimeException("Bukan giliran Anda.");

        List<GameTile> poolTiles = gameTileRepository.findByGameIdAndLocation(gameId, TileLocation.POOL);
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
}
