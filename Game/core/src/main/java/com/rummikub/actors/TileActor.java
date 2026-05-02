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
    // Drag callback interface
    // -------------------------------------------------------------------------

    /**
     * Callback for live drag position tracking.
     * GameScreen sets this to highlight bounding boxes during drag.
     */
    public interface DragMoveListener {
        void onDragMove(TileActor actor, float stageX, float stageY);
        void onDragEnd(TileActor actor, float stageX, float stageY);
    }

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
    private DragMoveListener dragMoveListener;

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
            private Actor dragProxy = null;

            @Override
            public void dragStart(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                  float x, float y, int pointer) {
                if (!strategy.isDraggable()) return;
                dragging = true;
                
                com.badlogic.gdx.math.Vector2 stagePos = localToStageCoordinates(new com.badlogic.gdx.math.Vector2(0, 0));
                
                dragProxy = com.rummikub.factory.TileActorFactory.create(tileData, strategy);
                dragProxy.setPosition(stagePos.x, stagePos.y);
                getStage().addActor(dragProxy);
                
                TileActor.this.setVisible(false);
            }

            @Override
            public void drag(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                             float x, float y, int pointer) {
                if (!dragging) return;
                
                if (dragProxy != null) {
                    dragProxy.setPosition(event.getStageX() - getWidth() / 2f, event.getStageY() - getHeight() / 2f);
                }

                if (dragMoveListener != null) {
                    dragMoveListener.onDragMove(TileActor.this, event.getStageX(), event.getStageY());
                }
            }

            @Override
            public void dragStop(com.badlogic.gdx.scenes.scene2d.InputEvent event,
                                 float x, float y, int pointer) {
                if (!dragging) return;
                
                if (dragProxy != null) {
                    dragProxy.remove();
                    dragProxy = null;
                }
                
                TileActor.this.setVisible(true);
                dragging = false;

                if (dragMoveListener != null) {
                    dragMoveListener.onDragEnd(TileActor.this, event.getStageX(), event.getStageY());
                }
                
                fire(new TileDropEvent(TileActor.this, event.getStageX(), event.getStageY()));
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

        // KRITIS: Akhiri batch dulu sebelum ShapeRenderer
        batch.end();

        // Sinkronkan projection dan transform matrix agar koordinat SR sama dengan Batch
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.setTransformMatrix(batch.getTransformMatrix());

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
        // Gunakan x, y yang sama (lokal terhadap parent group)
        font.getData().setScale(strategy.getFontScale());
        font.setColor(textColor);
        font.draw(batch, label, x + w * 0.18f, y + h * 0.68f);
        
        // Reset scale agar tidak mengganggu render lain
        font.getData().setScale(1f);
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

    public void setDragMoveListener(DragMoveListener listener) {
        this.dragMoveListener = listener;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
    }
}
