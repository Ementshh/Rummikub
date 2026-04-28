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
            sets.remove(sourceSetIndex);
            // If source was before target, the target index shifts down by one
            int adjustedTarget = targetSetIndex > sourceSetIndex
                    ? targetSetIndex - 1
                    : targetSetIndex;
            TableSetDto target = sets.get(adjustedTarget);
            target.tile_ids.add(tileId);
            target.set_type = gsm.detectSetType(target.tile_ids);
        } else {
            source.set_type = gsm.detectSetType(source.tile_ids);
            TableSetDto target = sets.get(targetSetIndex);
            target.tile_ids.add(tileId);
            target.set_type = gsm.detectSetType(target.tile_ids);
        }
    }

    @Override
    public void undo() {
        List<TableSetDto> sets = gsm.getTableSets();

        // Determine the actual current index of the target set
        int currentTarget = (sourceSetRemoved && targetSetIndex > sourceSetIndex)
                ? targetSetIndex - 1
                : targetSetIndex;

        TableSetDto target = sets.get(currentTarget);
        target.tile_ids.remove(Integer.valueOf(tileId));
        if (!target.tile_ids.isEmpty()) {
            target.set_type = gsm.detectSetType(target.tile_ids);
        } else {
            sets.remove(currentTarget);
        }

        if (sourceSetRemoved) {
            // Recreate the source set with just this tile
            TableSetDto restored = new TableSetDto("RUN", new java.util.ArrayList<>());
            restored.tile_ids.add(tileId);
            restored.set_type = gsm.detectSetType(restored.tile_ids);
            sets.add(sourceSetIndex, restored);
        } else {
            TableSetDto src = sets.get(sourceSetIndex);
            src.tile_ids.add(tileId);
            src.set_type = gsm.detectSetType(src.tile_ids);
        }

        sourceSetRemoved = false;
    }
}
