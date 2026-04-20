package com.rummikub.screens.states;

import com.rummikub.screens.GameScreen;
import com.rummikub.screens.GameScreenState;
import com.rummikub.state.GameStateManager;

/**
 * [STATE] — Active while it is this player's turn.
 * Enables controls, takes a snapshot for undo, and counts down a 120-second timer.
 */
public class MyTurnState implements GameScreenState {

    private float turnTimer = 120f;

    @Override
    public void enter(GameScreen screen) {
        screen.setControlsEnabled(true);
        GameStateManager.getInstance().takeSnapshot();
    }

    @Override
    public void update(GameScreen screen, float delta) {
        turnTimer -= delta;
        screen.updateTimerDisplay((int) turnTimer);
        if (turnTimer <= 0f) {
            screen.onTimerExpired();
        }
    }

    @Override
    public void exit(GameScreen screen) {
        turnTimer = 120f;
    }
}
