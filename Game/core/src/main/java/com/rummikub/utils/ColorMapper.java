package com.rummikub.utils;

import com.badlogic.gdx.graphics.Color;

/**
 * Maps server-side tile color strings to LibGDX Color instances.
 */
public final class ColorMapper {

    private ColorMapper() {}

    public static Color toLibGDX(String colorName) {
        if (colorName == null) return Color.GRAY;
        switch (colorName.toUpperCase()) {
            case "RED":    return new Color(0.85f, 0.15f, 0.15f, 1f);
            case "BLUE":   return new Color(0.15f, 0.35f, 0.85f, 1f);
            case "YELLOW": return new Color(0.95f, 0.80f, 0.10f, 1f);
            case "BLACK":  return new Color(0.10f, 0.10f, 0.10f, 1f);
            default:       return Color.GRAY;
        }
    }
}
