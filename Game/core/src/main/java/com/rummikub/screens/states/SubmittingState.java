package com.rummikub.screens.states;

import com.badlogic.gdx.Gdx;
import com.rummikub.screens.GameScreen;
import com.rummikub.screens.GameScreenState;

/**
 * [STATE] — Active while the end-turn request is in-flight.
 * Disables controls and shows a loading overlay until the async callback returns.
 */
public class SubmittingState implements GameScreenState {

    @Override
    public void enter(GameScreen screen) {
        screen.setControlsEnabled(false);
        Gdx.app.log("SubmittingState", "Showing loading overlay");
        // TODO: show loading overlay actor on screen
    }

    @Override
    public void update(GameScreen screen, float delta) {
        // Nothing to do — waiting for async callback
    }

    @Override
    public void exit(GameScreen screen) {
        Gdx.app.log("SubmittingState", "Hiding loading overlay");
        // TODO: hide loading overlay actor on screen
    }
}
