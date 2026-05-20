package com.rummikub.actors;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.rummikub.network.dto.TileDto;
import com.rummikub.strategy.TileRenderStrategy;
import com.rummikub.utils.ResourcePool;


// Repreentasi satu tile. Menggunakan  {@link ShapeRenderer} and {@link BitmapFont} dari {@link ResourcePool}
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

    private TileRenderStrategy strategy;
    private boolean selected;
    private boolean dragging;
    private DragMoveListener dragMoveListener;

    private com.badlogic.gdx.graphics.g2d.TextureRegion tileRegion;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public TileActor(TileDto tileData,
                     TileRenderStrategy strategy) {
        this.tileData  = tileData;
        this.strategy  = strategy;

        String colorLower = tileData.color.toLowerCase();
        if (tileData.isJoker) {
            tileRegion = ResourcePool.getInstance().getTileAtlas().findRegion("joker_" + colorLower);
        } else {
            tileRegion = ResourcePool.getInstance().getTileAtlas().findRegion(colorLower + "_" + tileData.number);
        }

        float targetHeight = strategy.getTileHeight();
        float calculatedWidth = strategy.getTileWidth();

        if (tileRegion != null) {
            float aspectRatio = (float) tileRegion.getRegionWidth() / tileRegion.getRegionHeight();
            calculatedWidth = targetHeight * aspectRatio;
        }

        setSize(calculatedWidth, targetHeight);

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
                    if (dragProxy instanceof TileActor) {
                        ((TileActor) dragProxy).dispose();
                    }
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
    // Drawing — Gunakan share resources dari ResourcePool
    // -------------------------------------------------------------------------

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();

        if (tileRegion != null) {
            Color c = getColor();
            batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
            batch.draw(tileRegion, x, y, w, h);
        }
        if (selected || dragging) {
            batch.end();
            ShapeRenderer shapeRenderer = ResourcePool.getInstance().getShapeRenderer();
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
            
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(strategy.getBorderColor(selected, dragging));
            shapeRenderer.rect(x - 1, y - 1, w + 2, h + 2);
            shapeRenderer.end();
            
            batch.begin();
        }
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
        // shared resources are managed by ResourcePool
    }
}
