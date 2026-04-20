package com.rummikub.strategy;

import com.badlogic.gdx.graphics.Color;
import com.rummikub.utils.Constants;

/**
 * Render strategy for tiles sitting in the player's rack.
 * Full size, draggable, grey default border.
 */
public class RackTileStrategy implements TileRenderStrategy {

    @Override
    public float getTileWidth() {
        return Constants.TILE_WIDTH;
    }

    @Override
    public float getTileHeight() {
        return Constants.TILE_HEIGHT;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public Color getBorderColor(boolean isSelected, boolean isDragging) {
        if (isDragging)  return Color.YELLOW;
        if (isSelected)  return Color.WHITE;
        return new Color(0.6f, 0.6f, 0.6f, 1f);
    }

    @Override
    public float getFontScale() {
        return 1.8f;
    }
}
