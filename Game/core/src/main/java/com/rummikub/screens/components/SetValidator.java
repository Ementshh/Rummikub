package com.rummikub.screens.components;

import com.rummikub.network.dto.TileDto;
import com.rummikub.state.GameStateManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates tile sets locally on the client side.
 * Checks GROUP (same number, unique colors) and RUN (same color, sequential) validity.
 */
public class SetValidator {

    private final GameStateManager gsm;

    public SetValidator(GameStateManager gsm) {
        this.gsm = gsm;
    }

    /**
     * Checks if tile_ids form a valid GROUP (same number, unique colors, 3-4 tiles).
     */
    public boolean isValidGroup(List<Integer> tileIds) {
        if (tileIds == null || tileIds.size() < 3 || tileIds.size() > 4) return false;
        Set<String> colors = new HashSet<>();
        int expectedNumber = -1;
        for (int id : tileIds) {
            TileDto t = findTileById(id);
            if (t == null) return false;
            if (t.isJoker) continue;
            if (expectedNumber == -1) expectedNumber = t.number;
            else if (t.number != expectedNumber) return false;
            if (!colors.add(t.color)) return false; // Duplicate color
        }
        return true;
    }

    /**
     * Checks if tile_ids form a valid RUN (same color, sequential numbers).
     * Sorts by number before checking sequence.
     */
    public boolean isValidRun(List<Integer> tileIds) {
        if (tileIds == null || tileIds.size() < 3) return false;
        List<TileDto> nonJokers = new ArrayList<>();
        int jokerCount = 0;
        for (int id : tileIds) {
            TileDto t = findTileById(id);
            if (t == null) return false;
            if (t.isJoker) { jokerCount++; continue; }
            nonJokers.add(t);
        }
        if (nonJokers.isEmpty()) return true; // All jokers

        // Check same color
        String color = nonJokers.get(0).color;
        for (TileDto t : nonJokers) {
            if (!color.equals(t.color)) return false;
        }

        // Sort by number and check sequence with joker gaps
        nonJokers.sort((a, b) -> Integer.compare(a.number, b.number));

        // cek duplicate
        for (int i = 1; i < nonJokers.size(); i++) {
            if (nonJokers.get(i).number == nonJokers.get(i-1).number) {
                return false;
            }
        }

        int startNum = nonJokers.get(0).number;
        int endNum = nonJokers.get(nonJokers.size() - 1).number;
        int expectedLength = endNum - startNum + 1;
        
        if (expectedLength > nonJokers.size() + jokerCount) return false;
        
        if (nonJokers.size() + jokerCount > 13) return false;

        return true;
    }

    /**
     * Resolves a tile DTO by ID from the game state cache, with fallbacks.
     */
    public TileDto findTileById(int id) {
        TileDto cached = gsm.getTileById(id);
        if (cached != null) return cached;

        // Fallback: linear search in rack
        for (TileDto t : gsm.getMyRackTiles()) {
            if (t.id == id) return t;
        }

        // Last resort: placeholder so the display doesn't crash
        TileDto placeholder = new TileDto();
        placeholder.id = id;
        placeholder.color = "BLACK";
        placeholder.number = 0;
        placeholder.isJoker = false;
        return placeholder;
    }
}
// hi