package com.rummikub.screens;

/**
 * [STATE] — Interface for all GameScreen states.
 * Each state controls what the player can do and how the screen behaves.
 */
public interface GameScreenState {
    void enter(GameScreen screen);
    void update(GameScreen screen, float delta);
    void exit(GameScreen screen);
}
