package com.rummikub.strategy;

import com.badlogic.gdx.graphics.Color;
import com.rummikub.utils.Constants;

/**
 * Strategy for tiles that cannot be moved.
 */
public class LockedTileStrategy implements TileRenderStrategy {
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
        return false;
    }

    @Override
    public Color getBorderColor(boolean isSelected, boolean isDragging) {
        return Color.DARK_GRAY;
    }

    @Override
    public float getFontScale() {
        return 0.7f;
    }
}
