package com.rummikub.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.rummikub.network.dto.TileDto;
import com.rummikub.strategy.TileRenderStrategy;

/**
 * Visual representation of a single Rummikub tile.
 *
 * Rendering is delegated to a {@link TileRenderStrategy} so the same actor
 * class can be used for both rack tiles and table tiles without subclassing.
 *
 * Uses its own {@link ShapeRenderer} for the background/border and a
 * {@link BitmapFont} for the number label. Both are disposed in
 * {@link #dispose()}.
 */
public class TileActor extends Actor {

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final TileDto tileData;
    private final Color bgColor;
    private final Color textColor;
    private final String label;

    private TileRenderStrategy strategy;
    private boolean selected;
    private boolean dragging;

    /** Owned by this actor — must be disposed. */
    private final ShapeRenderer shapeRenderer;
    /** Owned by this actor — must be disposed. */
    private final BitmapFont font;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public TileActor(TileDto tileData,
                     Color bgColor,
                     Color textColor,
                     String label,
                     TileRenderStrategy strategy) {
        this.tileData  = tileData;
        this.bgColor   = bgColor;
        this.textColor = textColor;
        this.label     = label;
        this.strategy  = strategy;

        setSize(strategy.getTileWidth(), strategy.getTileHeight());

        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        setupDragListener();
    }

    // -------------------------------------------------------------------------
    // Drag listener
    // -------------------------------------------------------------------------

    private void setupDragListener() {
        addListener(new DragListener() {

            @Override
            public void dragStart(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                  float x, float y, int pointer) {
                dragging = true;
                toFront();
            }

            @Override
            public void drag(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                             float x, float y, int pointer) {
                if (strategy.isDraggable()) {
                    moveBy(x - getWidth() / 2f, y - getHeight() / 2f);
                }
            }

            @Override
            public void dragStop(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                 float x, float y, int pointer) {
                dragging = false;
                fire(new TileDropEvent(TileActor.this, getX(), getY()));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();

        // ShapeRenderer requires the SpriteBatch to be inactive
        batch.end();

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());

        // --- Filled background ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(x, y, w, h);
        shapeRenderer.end();

        // --- Border ---
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(strategy.getBorderColor(selected, dragging));
        shapeRenderer.rect(x - 1, y - 1, w + 2, h + 2);
        shapeRenderer.end();

        batch.begin();

        // --- Text label ---
        font.getData().setScale(strategy.getFontScale());
        font.setColor(textColor);
        font.draw(batch, label, x + w * 0.15f, y + h * 0.65f);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public TileDto getTileData() {
        return tileData;
    }

    public boolean isJoker() {
        return tileData != null && tileData.isJoker;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public TileRenderStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(TileRenderStrategy strategy) {
        this.strategy = strategy;
        setSize(strategy.getTileWidth(), strategy.getTileHeight());
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
    }
}
