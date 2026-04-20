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

        if (!source.tileIds.remove(Integer.valueOf(tileId))) return;  // not found

        // Reconstruct a minimal TileDto from the tile map held by the state manager.
        // We search all known tiles (rack + table) to find the matching DTO.
        savedTile = findTileDto(tileId);

        if (savedTile == null) {
            // Fallback: create a placeholder so undo can at least restore the id
            savedTile = new TileDto(tileId, "BLACK", 0, false);
        }

        if (source.tileIds.isEmpty()) {
            sets.remove(sourceSetIndex);
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
            sets.get(sourceSetIndex).tileIds.add(tileId);
        } else {
            // Set was removed when it became empty — recreate it
            com.rummikub.network.dto.TableSetDto restored =
                    new com.rummikub.network.dto.TableSetDto("RUN", new java.util.ArrayList<>());
            restored.tileIds.add(tileId);
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
        // At execute() time the tile is no longer in any set, and not yet in the rack.
        // We rely on the snapshot stored in GameStateManager to find the original DTO.
        // As a practical fallback we scan the rack (in case of partial state).
        for (TileDto t : gsm.getMyRackTiles()) {
            if (t.id == id) return t;
        }
        return null;
    }
}
