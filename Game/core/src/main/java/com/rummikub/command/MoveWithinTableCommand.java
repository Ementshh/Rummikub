package com.rummikub.command;

import com.rummikub.network.dto.TableSetDto;
import com.rummikub.state.GameStateManager;

import java.util.List;

/**
 * Moves a tile from one set on the table to another set on the table.
 */
public class MoveWithinTableCommand implements TileCommand {

    private final int tileId;
    private final int sourceSetIndex;
    private final int targetSetIndex;

    private final GameStateManager gsm = GameStateManager.getInstance();

    /** Whether the source set was removed (became empty) during execute(). */
    private boolean sourceSetRemoved = false;

    public MoveWithinTableCommand(int tileId, int sourceSetIndex, int targetSetIndex) {
        this.tileId          = tileId;
        this.sourceSetIndex  = sourceSetIndex;
        this.targetSetIndex  = targetSetIndex;
    }

    @Override
    public void execute() {
        List<TableSetDto> sets = gsm.getTableSets();
        TableSetDto source = sets.get(sourceSetIndex);

        if (!source.tile_ids.remove(Integer.valueOf(tileId))) return;  // tile not found

        sourceSetRemoved = source.tile_ids.isEmpty();
        if (sourceSetRemoved) {
            // Keep empty set as gap placeholder instead of removing
            source.set_type = null;
            source.isNewThisTurn = false;

            // Target index unchanged since we didn't remove the source
            TableSetDto target = sets.get(targetSetIndex);
            target.tile_ids.add(tileId);
            if (target.isNewThisTurn) {
                target.set_type = gsm.detectSetType(target.tile_ids);
            }
        } else {
            if (source.isNewThisTurn) {
                source.set_type = gsm.detectSetType(source.tile_ids);
            }
            TableSetDto target = sets.get(targetSetIndex);
            target.tile_ids.add(tileId);
            if (target.isNewThisTurn) {
                target.set_type = gsm.detectSetType(target.tile_ids);
            }
        }
    }

    @Override
    public void undo() {
        List<TableSetDto> sets = gsm.getTableSets();

        // Target index unchanged since we preserve gaps
        TableSetDto target = sets.get(targetSetIndex);
        target.tile_ids.remove(Integer.valueOf(tileId));
        if (!target.tile_ids.isEmpty()) {
            if (target.isNewThisTurn) {
                target.set_type = gsm.detectSetType(target.tile_ids);
            }
        }
        // Note: we don't remove empty target sets to preserve grid positions

        if (sourceSetRemoved) {
            // Restore tile to the empty source set (gap)
            TableSetDto src = sets.get(sourceSetIndex);
            src.tile_ids.add(tileId);
            src.isNewThisTurn = true;
            src.set_type = gsm.detectSetType(src.tile_ids);
        } else {
            TableSetDto src = sets.get(sourceSetIndex);
            src.tile_ids.add(tileId);
            if (src.isNewThisTurn) {
                src.set_type = gsm.detectSetType(src.tile_ids);
            }
        }

        sourceSetRemoved = false;
    }
}
