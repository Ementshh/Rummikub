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
            // Convert to empty placeholder instead of removing
            source.set_type = null;
            source.isNewThisTurn = false;
        } else {
            if (source.isNewThisTurn) {
                source.set_type = gsm.detectSetType(source.tile_ids);
            }
        }

        gsm.removePlacedTile(tileId);
        gsm.getMyRackTiles().add(savedTile);
    }

    @Override
    public void undo() {
        if (savedTile == null) return;

        // Remove from rack
        gsm.getMyRackTiles().remove(savedTile);

        List<TableSetDto> sets = gsm.getTableSets();

        // Re-insert into the original set at that index
        // The set should still exist (as empty placeholder) due to gap-preserving logic
        if (sourceSetIndex < sets.size()) {
            TableSetDto target = sets.get(sourceSetIndex);
            target.tile_ids.add(tileId);
            target.isNewThisTurn = true;
            target.set_type = gsm.detectSetType(target.tile_ids);
            gsm.addPlacedTile(tileId);
        } else {
            // Fallback: recreate at position if somehow missing
            TableSetDto restored = new TableSetDto("RUN", new java.util.ArrayList<>());
            restored.isNewThisTurn = true;
            restored.tile_ids.add(tileId);
            restored.set_type = gsm.detectSetType(restored.tile_ids);
            gsm.addPlacedTile(tileId);
            // Fill gaps if needed
            while (sets.size() < sourceSetIndex) {
                sets.add(TableSetDto.createEmpty());
            }
            if (sourceSetIndex < sets.size()) {
                sets.set(sourceSetIndex, restored);
            } else {
                sets.add(restored);
            }
        }

        savedTile = null;
    }


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
