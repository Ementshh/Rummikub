package com.rummikub.screens.states;

import com.badlogic.gdx.Gdx;
import com.rummikub.screens.GameScreen;
import com.rummikub.screens.GameScreenState;

/**
 * [STATE] — Active while it is not this player's turn.
 * Polls the server every 2 seconds to check if the turn has changed.
 */
public class WaitingTurnState implements GameScreenState {

    private float pollTimer = 0f;

    @Override
    public void enter(GameScreen screen) {
        Gdx.app.log("WaitingTurnState", "Waiting for turn...");
        screen.setControlsEnabled(false);
    }

    @Override
    public void update(GameScreen screen, float delta) {
        pollTimer += delta;
        if (pollTimer >= 2f) {
            pollTimer = 0f;
            screen.pollGameState();
        }
    }

    @Override
    public void exit(GameScreen screen) {
        // No special action needed on exit
    }
}
