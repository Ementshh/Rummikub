package com.rummikub.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.rummikub.RummikubGame;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
        @Override
        public GwtApplicationConfiguration getConfig () {
        GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(1280, 720);
        cfg.padVertical = 0;
        cfg.padHorizontal = 0;
        
        cfg.useGL30 = true; 
        
        return cfg;
    }

        @Override
        public ApplicationListener createApplicationListener () {
            return new RummikubGame();
        }
}