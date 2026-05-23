package com.rummikub.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.rummikub.RummikubGame;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
        @Override
        public GwtApplicationConfiguration getConfig () {
            // Disable physical pixels to avoid texture scaling bugs on some retina web displays
            GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(false);
            cfg.padVertical = 0;
            cfg.padHorizontal = 0;
            // Explicitly disable WebGL 2.0 to prevent the 'drawElementsInstanced' index buffer crash
            cfg.useGL30 = false;
            return cfg;
        }
        @Override
        public ApplicationListener createApplicationListener () {
            return new RummikubGame();
        }
}