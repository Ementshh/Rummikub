package com.rummikub;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class RummikubGame extends Game {
    @Override
    public void create() {
        // Init singletons
        // NetworkManager.getInstance().init();
        // GameStateManager.getInstance().init();
        // Mulai dari login screen
        // setScreen(new LoginScreen(this));
    }
    
    @Override
    public void render() {
        Gdx.gl.glClearColor(0.13f, 0.18f, 0.13f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render();
    }
}
