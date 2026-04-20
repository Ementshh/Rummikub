package com.rummikub.utils;

import com.badlogic.gdx.graphics.Color;

public class Constants {
    public static final String BASE_URL = "http://localhost:3000";
    public static final int SCREEN_WIDTH = 1280;
    public static final int SCREEN_HEIGHT = 720;
    public static final int TILE_WIDTH = 60;
    public static final int TILE_HEIGHT = 80;
    public static final int RACK_Y = 20;
    public static final int POLL_INTERVAL_SECONDS = 2;
    public static final int TURN_TIMER_SECONDS = 120;
    
    // Warna tile sebagai LibGDX Color
    public static final Color COLOR_RED    = new Color(0.85f, 0.15f, 0.15f, 1f);
    public static final Color COLOR_BLUE   = new Color(0.15f, 0.35f, 0.85f, 1f);
    public static final Color COLOR_YELLOW = new Color(0.95f, 0.80f, 0.10f, 1f);
    public static final Color COLOR_BLACK  = new Color(0.10f, 0.10f, 0.10f, 1f);
    public static final Color COLOR_JOKER  = new Color(0.60f, 0.10f, 0.80f, 1f);
}
