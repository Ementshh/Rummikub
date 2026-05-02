package com.rummikub.command;

import com.rummikub.network.dto.TableSetDto;
import com.rummikub.network.dto.TileDto;
import com.rummikub.state.GameStateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves a tile from the player's rack to the table.
 *
 * If {@code toNewSet} is true a brand-new {@link TableSetDto} is created;
 * otherwise the tile is appended to the existing set at {@code targetSetIndex}.
 */
public class PlaceTileCommand implements TileCommand {

    private final int tileId;
    private final boolean toNewSet;
    private final int targetSetIndex;   // ignored when toNewSet == true
    private final String setType;       // "RUN" or "GROUP", only used for new sets

    private final GameStateManager gsm = GameStateManager.getInstance();

    /** Saved during execute() so undo() can restore the exact TileDto. */
    private TileDto savedTile;

    public PlaceTileCommand(int tileId, boolean toNewSet, int targetSetIndex, String setType) {
        this.tileId         = tileId;
        this.toNewSet       = toNewSet;
        this.targetSetIndex = targetSetIndex;
        this.setType        = setType;
    }

    @Override
    public void execute() {
        List<TileDto> rack = gsm.getMyRackTiles();

        // Find and remove the tile from the rack
        TileDto found = null;
        for (TileDto t : rack) {
            if (t.id == tileId) {
                found = t;
                break;
            }
        }
        if (found == null) return;   // tile not in rack — nothing to do

        savedTile = found;
        rack.remove(found);

        List<TableSetDto> sets = gsm.getTableSets();

        if (toNewSet) {
            TableSetDto newSet = new TableSetDto(setType, new ArrayList<>());
            newSet.isNewThisTurn = true; // set baru = boleh di-detect
            newSet.tile_ids.add(tileId);
            newSet.set_type = gsm.detectSetType(newSet.tile_ids);
            sets.add(newSet);
        } else {
            TableSetDto target = sets.get(targetSetIndex);
            target.tile_ids.add(tileId);
            // Hanya update set_type untuk set yang BARU dibuat pemain ini
            if (target.isNewThisTurn) {
                target.set_type = gsm.detectSetType(target.tile_ids);
            }
        }
    }

    @Override
    public void undo() {
        if (savedTile == null) return;

        List<TableSetDto> sets = gsm.getTableSets();

        if (toNewSet) {
            // The new set was appended last — find and remove it
            for (int i = sets.size() - 1; i >= 0; i--) {
                TableSetDto s = sets.get(i);
                if (s.tile_ids.size() == 1 && s.tile_ids.get(0) == tileId) {
                    sets.remove(i);
                    break;
                }
            }
        } else {
            TableSetDto target = sets.get(targetSetIndex);
            target.tile_ids.remove(Integer.valueOf(tileId));
            if (target.tile_ids.isEmpty()) {
                sets.remove(targetSetIndex);
            } else {
                if (target.isNewThisTurn) {
                    target.set_type = gsm.detectSetType(target.tile_ids);
                }
            }
        }

        gsm.getMyRackTiles().add(savedTile);
        savedTile = null;
    }
}
