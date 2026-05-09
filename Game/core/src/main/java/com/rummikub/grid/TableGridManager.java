package com.rummikub.grid;

import com.badlogic.gdx.Gdx;
import com.rummikub.network.dto.TableSetDto;
import com.rummikub.screens.components.SetValidator;
import com.rummikub.state.GameStateManager;

import java.util.List;

// manajer grid table: jumlah kolom dan logika ekspansi
public class TableGridManager {

    private static final int GRID_ROWS = 4;
    private static final int INITIAL_COLUMNS = 2;

    private final GameStateManager gsm;
    private final SetValidator setValidator;

    private int numCols = INITIAL_COLUMNS;

    public TableGridManager(GameStateManager gsm, SetValidator setValidator) {
        this.gsm = gsm;
        this.setValidator = setValidator;
    }

    // return jumlah  kolom dalam grid
    public int getNumColumns() {
        return numCols;
    }

    // return banyak slots
    public int getTotalSlots() {
        return numCols * GRID_ROWS;
    }

    // Check paling kanan bawah untuk expand
    public void checkAndExpandGrid() {
        int bottomRightSlotIndex = getTotalSlots() - 1;

        List<TableSetDto> sets = gsm.getTableSets();

        // jika size sets <= bottomRightSlotIndex, artinya slot belum ada
        if (sets.size() <= bottomRightSlotIndex) {
            return;
        }

        TableSetDto bottomRightSet = sets.get(bottomRightSlotIndex);

        if (bottomRightSet == null || bottomRightSet.isEmpty()) {
            return;
        }

        // jika size tile_ids < 3, artinya set tidak valid
        if (bottomRightSet.tile_ids.size() < 3) {
            return;
        }

        boolean isValid;
        String setType = bottomRightSet.set_type != null ? bottomRightSet.set_type : "RUN";

        if ("GROUP".equals(setType)) {
            isValid = setValidator.isValidGroup(bottomRightSet.tile_ids);
        } else {
            isValid = setValidator.isValidRun(bottomRightSet.tile_ids);
        }

        if (isValid) {
            expandGrid();
        }
    }

    // add kolom baru ke grid
    private void expandGrid() {
        numCols++;
        Gdx.app.log("TableGridManager", "Grid expanded to " + numCols + " columns (" + getTotalSlots() + " total slots)");
    }

    // reset grid ke initial state
    public void reset() {
        numCols = INITIAL_COLUMNS;
        // pastikan cukup kolom untuk existing sets
        ensureColumnsForExistingSets();
    }

    // pastikan cukup kolom untuk existing sets
    public void ensureColumnsForExistingSets() {
        List<TableSetDto> sets = gsm.getTableSets();
        int requiredSlots = Math.max(sets.size(), INITIAL_COLUMNS * GRID_ROWS);
        int requiredCols = (int) Math.ceil((double) requiredSlots / GRID_ROWS);
        numCols = Math.max(numCols, requiredCols);
    }

    // return row index (0-3) untuk slot index tertentu
    public static int getRow(int slotIndex) {
        return slotIndex % GRID_ROWS;
    }

    // return column index untuk slot index tertentu
    public static int getColumn(int slotIndex) {
        return slotIndex / GRID_ROWS;
    }

    // return slot index untuk row dan column tertentu
    public static int getSlotIndex(int row, int col) {
        return col * GRID_ROWS + row;
    }
}
