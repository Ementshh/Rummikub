package com.rummikub;

import com.badlogic.gdx.Game;
import com.rummikub.screens.LoginScreen;

public class RummikubGame extends Game {

    @Override
    public void create() {
        // Init singletons (network / state) will be wired in later phases
        // NetworkManager.getInstance().init();
        // GameStateManager.getInstance().init();
        setScreen(new LoginScreen(this));
    }

    // render() is intentionally removed — Game.render() delegates to the
    // active Screen, and BaseScreen.render() handles clearing + drawing.
}
