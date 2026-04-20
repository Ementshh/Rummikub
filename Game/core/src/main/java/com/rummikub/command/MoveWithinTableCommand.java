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

        if (!source.tileIds.remove(Integer.valueOf(tileId))) return;  // tile not found

        sourceSetRemoved = source.tileIds.isEmpty();
        if (sourceSetRemoved) {
            sets.remove(sourceSetIndex);
            // If source was before target, the target index shifts down by one
            int adjustedTarget = targetSetIndex > sourceSetIndex
                    ? targetSetIndex - 1
                    : targetSetIndex;
            sets.get(adjustedTarget).tileIds.add(tileId);
        } else {
            sets.get(targetSetIndex).tileIds.add(tileId);
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
        target.tileIds.remove(Integer.valueOf(tileId));

        if (sourceSetRemoved) {
            // Recreate the source set with just this tile
            TableSetDto restored = new TableSetDto("RUN", new java.util.ArrayList<>());
            restored.tileIds.add(tileId);
            sets.add(sourceSetIndex, restored);
        } else {
            sets.get(sourceSetIndex).tileIds.add(tileId);
        }

        sourceSetRemoved = false;
    }
}
