package com.rummikub.command;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Maintains an ordered stack of executed {@link TileCommand}s so the player
 * can undo individual moves or reset the entire turn.
 */
public class CommandHistory {

    private final Deque<TileCommand> history = new ArrayDeque<>();

    /** Executes the command and pushes it onto the undo stack. */
    public void execute(TileCommand cmd) {
        cmd.execute();
        history.push(cmd);
    }

    /** Returns true if there is at least one command that can be undone. */
    public boolean canUndo() {
        return !history.isEmpty();
    }

    /** Undoes the most recently executed command. */
    public void undo() {
        if (canUndo()) {
            history.pop().undo();
        }
    }

    /** Undoes all commands in reverse order (full turn reset). */
    public void undoAll() {
        while (canUndo()) {
            history.pop().undo();
        }
    }

    /** Clears the history without undoing (e.g., after a successful end-turn). */
    public void clear() {
        history.clear();
    }
}
