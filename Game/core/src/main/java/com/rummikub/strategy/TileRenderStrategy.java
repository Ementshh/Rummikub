package com.rummikub.strategy;

import com.badlogic.gdx.graphics.Color;

/**
 * [STRATEGY] — Defines how a tile should be rendered and behave.
 * Different strategies are used for rack tiles vs. table tiles.
 */
public interface TileRenderStrategy {

    float getTileWidth();

    float getTileHeight();

    boolean isDraggable();

    /**
     * Returns the border color based on the tile's current interaction state.
     *
     * @param isSelected true if the tile is currently selected
     * @param isDragging true if the tile is currently being dragged
     */
    Color getBorderColor(boolean isSelected, boolean isDragging);

    float getFontScale();
}
