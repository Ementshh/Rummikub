package com.rummikub.screens;

import com.badlogic.gdx.Gdx;
import com.rummikub.RummikubGame;

/**
 * Main game screen — stub for Phase 2.
 * Hosts the state machine (WaitingTurnState / MyTurnState / SubmittingState / GameOverState).
 * Full implementation in a later phase.
 */
public class GameScreen extends BaseScreen {

    private GameScreenState currentState;

    public GameScreen(RummikubGame game) {
        super(game);
    }

    @Override
    protected void buildUI() {
        // Stub — UI will be built in a later phase
    }

    @Override
    protected void update(float delta) {
        if (currentState != null) {
            currentState.update(this, delta);
        }
    }

    // -------------------------------------------------------------------------
    // State machine
    // -------------------------------------------------------------------------

    /** Transitions to a new state, calling exit() on the old and enter() on the new. */
    public void transitionTo(GameScreenState newState) {
        if (currentState != null) {
            currentState.exit(this);
        }
        currentState = newState;
        if (currentState != null) {
            currentState.enter(this);
        }
    }

    // -------------------------------------------------------------------------
    // State callbacks — stubs to be implemented in later phases
    // -------------------------------------------------------------------------

    /** Enables or disables interactive controls based on whose turn it is. */
    public void setControlsEnabled(boolean enabled) {
        Gdx.app.log("GameScreen", "setControlsEnabled: " + enabled);
    }

    /** Polls the server for the latest game state. */
    public void pollGameState() {
        Gdx.app.log("GameScreen", "pollGameState()");
    }

    /** Updates the on-screen countdown timer label. */
    public void updateTimerDisplay(int seconds) {
        Gdx.app.log("GameScreen", "updateTimerDisplay: " + seconds);
    }

    /** Called when the turn timer reaches zero. */
    public void onTimerExpired() {
        Gdx.app.log("GameScreen", "onTimerExpired()");
    }

    /** Shows the game-over result panel. */
    public void showGameOverPanel() {
        Gdx.app.log("GameScreen", "showGameOverPanel()");
    }
}
