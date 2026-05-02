package com.rummikub.backend.services;

import com.rummikub.backend.models.GameParticipant;
import com.rummikub.backend.models.GameTile;
import com.rummikub.backend.models.TableSet;
import com.rummikub.backend.models.enums.GameStatus;
import com.rummikub.backend.models.enums.SetType;
import com.rummikub.backend.models.enums.TileLocation;
import com.rummikub.backend.repositories.GameParticipantRepository;
import com.rummikub.backend.repositories.GameRepository;
import com.rummikub.backend.repositories.GameTileRepository;
import com.rummikub.backend.repositories.TableSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TurnValidatorService {

    @Autowired
    private GameTileRepository gameTileRepository;

    @Autowired
    private TableSetRepository tableSetRepository;

    @Autowired
    private GameParticipantRepository gameParticipantRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private RummikubLogicService rummikubLogicService;

    public static class EndTurnResult {
        public boolean isFinished;
        public UUID winnerId;
        public EndTurnResult(boolean isFinished, UUID winnerId) { this.isFinished = isFinished; this.winnerId = winnerId; }
    }

    @Transactional(rollbackFor = Exception.class)
    public EndTurnResult executeEndTurn(String gameId, UUID participantId, List<RummikubLogicService.TableSetRequest> newTableSets, List<Integer> newRackTileIds) {
        
        List<GameTile> oldState = gameTileRepository.findBoardAndRackState(gameId, participantId);
        
        List<Integer> oldTableTiles = oldState.stream().filter(gt -> gt.getLocation() == TileLocation.TABLE).map(gt -> gt.getTile().getId()).toList();
        List<Integer> oldRackTiles = oldState.stream().filter(gt -> gt.getLocation() == TileLocation.RACK).map(gt -> gt.getTile().getId()).toList();
        
        List<Integer> allOldTiles = new ArrayList<>();
        allOldTiles.addAll(oldTableTiles);
        allOldTiles.addAll(oldRackTiles);

        List<Integer> allNewTableTiles = new ArrayList<>();
        for (RummikubLogicService.TableSetRequest set : newTableSets) {
            allNewTableTiles.addAll(set.tile_ids);
        }
        
        List<Integer> allNewTiles = new ArrayList<>();
        allNewTiles.addAll(allNewTableTiles);
        allNewTiles.addAll(newRackTileIds);

        List<Integer> oldSorted = new ArrayList<>(allOldTiles); Collections.sort(oldSorted);
        List<Integer> newSorted = new ArrayList<>(allNewTiles); Collections.sort(newSorted);

        if (oldSorted.size() != newSorted.size() || !oldSorted.equals(newSorted)) {
            throw new RuntimeException("Integritas ubin gagal: Jumlah ubin tidak sesuai dengan yang dimiliki di awal giliran.");
        }

        Set<Integer> tableOldSet = new HashSet<>(oldTableTiles);
        List<Integer> playerMovedTiles = new ArrayList<>();
        
        for (Integer tId : allNewTableTiles) {
            if (!tableOldSet.contains(tId)) {
                playerMovedTiles.add(tId);
            }
        }

        if (playerMovedTiles.isEmpty()) {
            throw new RuntimeException("Harus ada ubin yang diturunkan, atau tarik dari pool (draw).");
        }

        for (RummikubLogicService.TableSetRequest set : newTableSets) {
            RummikubLogicService.ValidationResult valRes = rummikubLogicService.isValidSet(SetType.valueOf(set.set_type), set.tile_ids);
            if (!valRes.valid) {
                throw new RuntimeException(valRes.reason);
            }
        }

        GameParticipant participant = gameParticipantRepository.findById(participantId).orElseThrow();
        if (!participant.isHasDoneInitialMeld()) {
            RummikubLogicService.ValidationResult meldCheck = rummikubLogicService.checkInitialMeld(newTableSets, playerMovedTiles, oldTableTiles);
            if (!meldCheck.valid) {
                throw new RuntimeException(meldCheck.reason);
            }
            participant.setScore(participant.getScore() + meldCheck.value);
            if (participant.getScore() >= 30) {
                participant.setHasDoneInitialMeld(true);
            }
            gameParticipantRepository.save(participant);
        }

        // Apply state:
        // Clear all old table sets for the game to recreate
        List<TableSet> oldSets = tableSetRepository.findByGameId(gameId);
        tableSetRepository.deleteAll(oldSets);
        
        // Unassign all involved game tiles
        for (GameTile gt : oldState) {
            gt.setLocation(TileLocation.POOL);
            gt.setParticipant(null);
            gt.setTableSet(null);
            gameTileRepository.save(gt);
        }

        // Create new table sets and assign tiles
        for (RummikubLogicService.TableSetRequest setReq : newTableSets) {
            TableSet newSet = new TableSet();
            newSet.setGame(participant.getGame());
            newSet.setSetType(SetType.valueOf(setReq.set_type));
            newSet = tableSetRepository.save(newSet);

            for (Integer tId : setReq.tile_ids) {
                GameTile gt = gameTileRepository.findByGameIdAndTileId(gameId, tId);
                gt.setLocation(TileLocation.TABLE);
                gt.setTableSet(newSet);
                gameTileRepository.save(gt);
            }
        }

        // Reassign rack tiles
        for (Integer tId : newRackTileIds) {
            GameTile gt = gameTileRepository.findByGameIdAndTileId(gameId, tId);
            gt.setLocation(TileLocation.RACK);
            gt.setParticipant(participant);
            gameTileRepository.save(gt);
        }

        if (newRackTileIds.isEmpty()) {
            var game = participant.getGame();
            game.setStatus(GameStatus.FINISHED);
            gameRepository.save(game);
            return new EndTurnResult(true, participantId);
        }

        return new EndTurnResult(false, null);
    }
}
