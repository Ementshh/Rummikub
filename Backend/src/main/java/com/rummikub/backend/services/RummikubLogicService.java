package com.rummikub.backend.services;

import com.rummikub.backend.models.Tile;
import com.rummikub.backend.models.enums.SetType;
import com.rummikub.backend.repositories.TileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RummikubLogicService {

    @Autowired
    private TileRepository tileRepository;

    private Map<Integer, Tile> tileDict = new HashMap<>();

    @PostConstruct
    public void init() {
        List<Tile> tiles = tileRepository.findAll();
        for (Tile t : tiles) {
            tileDict.put(t.getId(), t);
        }
    }

    public Tile getTile(Integer tileId) {
        return tileDict.get(tileId);
    }
    
    public Collection<Tile> getAllTiles() {
        return tileDict.values();
    }

    public static class ValidationResult {
        public boolean valid;
        public String reason;
        public int value;

        public ValidationResult(boolean valid, String reason, int value) {
            this.valid = valid;
            this.reason = reason;
            this.value = value;
        }
    }

    public ValidationResult isValidSet(SetType setType, List<Integer> tileIds) {
        if (tileIds == null || tileIds.size() < 3) return new ValidationResult(false, "Set minimum 3 ubin.", 0);
        if (tileIds.size() > 13) return new ValidationResult(false, "Set melebihi batas 13.", 0);

        List<Tile> tiles = new ArrayList<>();
        for (Integer id : tileIds) {
            tiles.add(getTile(id));
        }

        List<Tile> nonJokers = tiles.stream().filter(t -> !t.isJoker()).toList();
        if (nonJokers.isEmpty()) return new ValidationResult(true, "Semua joker", 30);

        if (setType == SetType.GROUP) {
            if (tileIds.size() > 4) return new ValidationResult(false, "Group maksimal 4 ubin.", 0);

            Integer targetNumber = nonJokers.get(0).getNumber();
            Set<String> colorsSeen = new HashSet<>();
            int valueSum = 0;

            for (Tile t : tiles) {
                if (t.isJoker()) {
                    valueSum += targetNumber;
                    continue;
                }
                if (!t.getNumber().equals(targetNumber)) return new ValidationResult(false, "Angka dalam Group berbeda.", 0);
                if (colorsSeen.contains(t.getColor().name())) return new ValidationResult(false, "Warna sama tidak valid dalam Group.", 0);
                colorsSeen.add(t.getColor().name());
                valueSum += t.getNumber();
            }
            return new ValidationResult(true, "OK", valueSum);

        } else if (setType == SetType.RUN) {
            String targetColor = nonJokers.get(0).getColor().name();
            int valueSum = 0;

            int currentNumber = -1;
            for (int i = 0; i < tiles.size(); i++) {
                if (!tiles.get(i).isJoker()) {
                    currentNumber = tiles.get(i).getNumber() - i;
                    break;
                }
            }

            if (currentNumber < 1) return new ValidationResult(false, "Run out of bounds di bawah 1.", 0);

            for (int i = 0; i < tiles.size(); i++) {
                Tile t = tiles.get(i);
                int expectedNumber = currentNumber + i;
                if (expectedNumber > 13) return new ValidationResult(false, "Run melebihi angka 13.", 0);

                if (!t.isJoker()) {
                    if (!t.getColor().name().equals(targetColor)) return new ValidationResult(false, "Beda warna dalam Run.", 0);
                    if (t.getNumber() != expectedNumber) return new ValidationResult(false, "Angka tidak berurutan.", 0);
                }
                valueSum += expectedNumber;
            }
            return new ValidationResult(true, "OK", valueSum);
        }

        return new ValidationResult(false, "Tipe set tidak dikenal.", 0);
    }

    public static class TableSetRequest {
        public String set_type;
        public List<Integer> tile_ids;
    }

    public ValidationResult checkInitialMeld(List<TableSetRequest> newTableSets, List<Integer> playerMovedTiles, List<Integer> oldBoardTiles) {
        int meldPoints = 0;
        Set<Integer> oldBoardSet = new HashSet<>(oldBoardTiles);
        Set<Integer> playerMovedSet = new HashSet<>(playerMovedTiles);

        for (TableSetRequest set : newTableSets) {
            boolean hasOld = false;
            boolean hasNew = false;
            for (Integer tId : set.tile_ids) {
                if (oldBoardSet.contains(tId)) hasOld = true;
                if (playerMovedSet.contains(tId)) hasNew = true;
            }

            if (hasOld && hasNew) {
                return new ValidationResult(false, "Initial Meld tidak boleh memodifikasi atau menempel pada set yang sudah ada di meja.", 0);
            }

            if (hasNew && !hasOld) {
                ValidationResult validationResult = isValidSet(SetType.valueOf(set.set_type), set.tile_ids);
                if (!validationResult.valid) {
                    return new ValidationResult(false, "Set baru dari rak tidak valid: " + validationResult.reason, 0);
                }
                meldPoints += validationResult.value;
            }
        }

        return new ValidationResult(true, "OK", meldPoints);
    }
}
