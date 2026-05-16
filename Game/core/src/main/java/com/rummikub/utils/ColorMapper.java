package com.rummikub.utils;

import com.badlogic.gdx.graphics.Color;


// Maps server-side tile color strings to LibGDX Color instances.


public final class ColorMapper {

    // Pre-allocated color constants (immutable references)
    private static final Color RED    = new Color(0.85f, 0.15f, 0.15f, 1f);
    private static final Color BLUE   = new Color(0.15f, 0.35f, 0.85f, 1f);
    private static final Color YELLOW = new Color(0.95f, 0.80f, 0.10f, 1f);
    private static final Color BLACK  = new Color(0.10f, 0.10f, 0.10f, 1f);

    private ColorMapper() {}

    public static Color toLibGDX(String colorName) {
        if (colorName == null) return Color.GRAY;
        switch (colorName.toUpperCase()) {
            case "RED":    return RED;
            case "BLUE":   return BLUE;
            case "YELLOW": return YELLOW;
            case "BLACK":  return BLACK;
            default:       return Color.GRAY;
        }
    }
}
