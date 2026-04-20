package com.rummikub.screens.states;

import com.rummikub.screens.GameScreen;
import com.rummikub.screens.GameScreenState;

/**
 * [STATE] — Active when the game has ended.
 * Triggers the game-over panel display.
 */
public class GameOverState implements GameScreenState {

    @Override
    public void enter(GameScreen screen) {
        screen.showGameOverPanel();
    }

    @Override
    public void update(GameScreen screen, float delta) {
        // Nothing to do
    }

    @Override
    public void exit(GameScreen screen) {
        // Nothing to do
    }
}
