package com.rummikub.command;

import com.rummikub.network.dto.TableSetDto;
import com.rummikub.network.dto.TileDto;
import com.rummikub.state.GameStateManager;

import java.util.List;

/**
 * Moves a tile from a set on the table back to the player's rack.
 */
public class ReturnTileCommand implements TileCommand {

    private final int tileId;
    private final int sourceSetIndex;

    private final GameStateManager gsm = GameStateManager.getInstance();

    /** Saved during execute() so undo() can restore the tile to the correct set. */
    private TileDto savedTile;

    public ReturnTileCommand(int tileId, int sourceSetIndex) {
        this.tileId          = tileId;
        this.sourceSetIndex  = sourceSetIndex;
    }

    @Override
    public void execute() {
        List<TableSetDto> sets = gsm.getTableSets();
        TableSetDto source = sets.get(sourceSetIndex);

        if (!source.tile_ids.remove(Integer.valueOf(tileId))) return;  // not found

        // Reconstruct a minimal TileDto from the tile map held by the state manager.
        // We search all known tiles (rack + table) to find the matching DTO.
        savedTile = findTileDto(tileId);

        if (savedTile == null) {
            // Fallback: create a placeholder so undo can at least restore the id
            savedTile = new TileDto(tileId, "BLACK", 0, false);
        }

        if (source.tile_ids.isEmpty()) {
            sets.remove(sourceSetIndex);
        } else {
            if (source.isNewThisTurn) {
                source.set_type = gsm.detectSetType(source.tile_ids);
            }
        }

        gsm.getMyRackTiles().add(savedTile);
    }

    @Override
    public void undo() {
        if (savedTile == null) return;

        // Remove from rack
        gsm.getMyRackTiles().remove(savedTile);

        List<TableSetDto> sets = gsm.getTableSets();

        // Re-insert into the original set (if it still exists at that index)
        if (sourceSetIndex < sets.size()) {
            TableSetDto target = sets.get(sourceSetIndex);
            target.tile_ids.add(tileId);
            if (target.isNewThisTurn) {
                target.set_type = gsm.detectSetType(target.tile_ids);
            }
        } else {
            // Set was removed when it became empty — recreate it
            TableSetDto restored = new TableSetDto("RUN", new java.util.ArrayList<>());
            restored.isNewThisTurn = true;
            restored.tile_ids.add(tileId);
            restored.set_type = gsm.detectSetType(restored.tile_ids);
            sets.add(sourceSetIndex, restored);
        }

        savedTile = null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Searches the rack for a TileDto with the given id.
     * (The tile has already been removed from the table set at this point,
     * but it hasn't been added to the rack yet, so we look in the rack snapshot
     * via the state manager's current rack list before the add.)
     */
    private TileDto findTileDto(int id) {
        // O(1) lookup from tile cache (includes rack + table tiles)
        TileDto cached = gsm.getTileById(id);
        if (cached != null) return cached;
        // Fallback: linear search in rack
        for (TileDto t : gsm.getMyRackTiles()) {
            if (t.id == id) return t;
        }
        return null;
    }
}
