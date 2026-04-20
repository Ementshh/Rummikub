package com.rummikub.strategy;

import com.badlogic.gdx.graphics.Color;
import com.rummikub.utils.Constants;

/**
 * Render strategy for tiles placed on the table.
 * Slightly smaller than rack tiles; cyan highlight when selected.
 */
public class TableTileStrategy implements TileRenderStrategy {

    @Override
    public float getTileWidth() {
        return Constants.TILE_WIDTH * 0.85f;
    }

    @Override
    public float getTileHeight() {
        return Constants.TILE_HEIGHT * 0.85f;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public Color getBorderColor(boolean isSelected, boolean isDragging) {
        if (isDragging)  return Color.YELLOW;
        if (isSelected)  return Color.CYAN;
        return new Color(0.4f, 0.4f, 0.4f, 1f);
    }

    @Override
    public float getFontScale() {
        return 1.5f;
    }
}
