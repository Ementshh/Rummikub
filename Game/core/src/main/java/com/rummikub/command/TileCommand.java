package com.rummikub.command;

/**
 * [COMMAND] — Base interface for all reversible tile operations.
 *
 * Every action the player takes during their turn (placing, returning,
 * or moving a tile) is wrapped in a TileCommand so it can be undone
 * via {@link CommandHistory}.
 */
public interface TileCommand {

    /** Applies the command to the current game state. */
    void execute();

    /** Reverses the effect of {@link #execute()}. */
    void undo();
}
