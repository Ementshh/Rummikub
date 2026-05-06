package com.rummikub.screens.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.rummikub.actors.TileActor;
import com.rummikub.factory.TileActorFactory;
import com.rummikub.network.dto.TableSetDto;
import com.rummikub.network.dto.TileDto;
import com.rummikub.state.GameStateManager;
import com.rummikub.strategy.LockedTileStrategy;
import com.rummikub.strategy.TableTileStrategy;
import com.rummikub.strategy.TileRenderStrategy;
import com.rummikub.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles rendering of tile sets on the table area.
 * Manages bounding boxes for drag-drop hit testing and highlight rendering.
 */
public class TableRenderer {

    private static final float BB_PADDING = 10f;

    private final Group tableGroup;
    private final ScrollPane tableScroll;
    private final GameStateManager gsm;
    private final SetValidator setValidator;
    private final float tableHeight;

    private final List<Rectangle> setBoundingBoxes = new ArrayList<>();
    private int highlightedSetIndex = -1;

    public TableRenderer(Group tableGroup, ScrollPane tableScroll,
                         GameStateManager gsm, SetValidator setValidator,
                         float tableHeight) {
        this.tableGroup = tableGroup;
        this.tableScroll = tableScroll;
        this.gsm = gsm;
        this.setValidator = setValidator;
        this.tableHeight = tableHeight;
    }

    /**
     * Rebuilds all table tile actors from current game state.
     * @param dragHandler used to attach drop/drag listeners to each created tile actor
     */
    public void rebuild(TileDragHandler dragHandler) {
        if (tableGroup == null) return;

        // Dispose old actors
        for (Actor a : tableGroup.getChildren()) {
            if (a instanceof TileActor) ((TileActor) a).dispose();
        }
        tableGroup.clearChildren();
        setBoundingBoxes.clear();
        highlightedSetIndex = -1;

        List<TableSetDto> sets = gsm.getTableSets();
        float setMargin = 12f;
        float tileW     = Constants.TILE_WIDTH * 0.85f + 2f;
        float tileH     = Constants.TILE_HEIGHT * 0.85f;
        float cursorX   = setMargin;
        float tileY     = (tableHeight - tileH) / 2f;

        for (int si = 0; si < sets.size(); si++) {
            TableSetDto set = sets.get(si);

            if (set.tile_ids == null || set.tile_ids.isEmpty()) {
                Gdx.app.log("TableRenderer", "set " + si + " is empty, skipping");
                cursorX += setMargin * 2;
                setBoundingBoxes.add(new Rectangle(0, 0, 0, 0));
                continue;
            }

            int tileCount = set.tile_ids.size();
            float setPixelW = tileCount * tileW;

            // Compute bounding box for this set (in tableGroup local coords)
            Rectangle bb = new Rectangle(
                cursorX - BB_PADDING,
                tileY - BB_PADDING,
                setPixelW + BB_PADDING * 2,
                tileH + BB_PADDING * 2
            );
            setBoundingBoxes.add(bb);

            // Determine label text and color based on set contents
            String labelText;
            Color labelColor;
            if (tileCount < 3) {
                labelText = "INCOMPLETE (" + tileCount + ")";
                labelColor = new Color(0.6f, 0.6f, 0.6f, 1f);
            } else {
                if (set.isNewThisTurn) {
                    set.set_type = gsm.detectSetType(set.tile_ids);
                }
                String detectedType = set.set_type != null ? set.set_type : "RUN";
                if ("GROUP".equals(detectedType)) {
                    boolean valid = setValidator.isValidGroup(set.tile_ids);
                    labelText = valid ? "GROUP \u2713" : "GROUP \u2717";
                    labelColor = valid ? new Color(0.2f, 0.85f, 0.2f, 1f) : new Color(0.9f, 0.2f, 0.2f, 1f);
                } else {
                    boolean valid = setValidator.isValidRun(set.tile_ids);
                    labelText = valid ? "RUN \u2713" : "RUN \u2717";
                    labelColor = valid ? new Color(0.2f, 0.85f, 0.2f, 1f) : new Color(0.9f, 0.2f, 0.2f, 1f);
                }
            }

            Label setLabel = createLabel(labelText);
            setLabel.setFontScale(0.65f);
            setLabel.setColor(labelColor);
            setLabel.setPosition(cursorX, tileY + tileH + 6);
            tableGroup.addActor(setLabel);

            for (int ti = 0; ti < set.tile_ids.size(); ti++) {
                int tileId = set.tile_ids.get(ti);
                TileDto dto = gsm.getTileById(tileId);

                if (dto == null) {
                    Gdx.app.log("RENDER_ERR", "TileDto not found in cache for id=" + tileId);
                    continue;
                }

                TileRenderStrategy strategy = new TableTileStrategy();
                // Apply LockedTileStrategy (dark border, but now draggable) to all committed tiles
                if (!set.isNewThisTurn) {
                    strategy = new LockedTileStrategy();
                }
                TileActor actor = TileActorFactory.create(dto, strategy);
                actor.setPosition(cursorX + ti * tileW, tileY);
                final int setIndex = si;
                boolean isCommitted = !set.isNewThisTurn;
                dragHandler.attachDropListener(actor, "TABLE", setIndex, isCommitted);
                dragHandler.attachDragMoveListener(actor, "TABLE", isCommitted);
                tableGroup.addActor(actor);
            }
            cursorX += setPixelW + setMargin * 2;
        }

        // "New set" drop zone at the end
        addNewSetDropZone(cursorX, tileY);
    }

    /**
     * Adds an invisible drop zone that creates a new set when a tile is dropped on it.
     */
    private void addNewSetDropZone(float x, float y) {
        Actor zone = new Actor();
        zone.setBounds(x, y, Constants.TILE_WIDTH, Constants.TILE_HEIGHT);
        zone.addListener(new DragListener() {
            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer) {
                // Drop zone — handled by TileDropEvent on the tile itself
            }
        });
        tableGroup.addActor(zone);
    }

    // -------------------------------------------------------------------------
    // Bounding box API
    // -------------------------------------------------------------------------

    /**
     * Returns the index of the set whose bounding box contains the given
     * tableGroup-local coordinates, or -1 if no match.
     */
    public int findSetIndexAt(float localX, float localY) {
        for (int i = 0; i < setBoundingBoxes.size(); i++) {
            Rectangle bb = setBoundingBoxes.get(i);
            if (bb.width > 0 && bb.contains(localX, localY)) {
                return i;
            }
        }
        return -1;
    }

    public int getHighlightedSetIndex() { return highlightedSetIndex; }
    public void setHighlightedSetIndex(int idx) { this.highlightedSetIndex = idx; }
    public ScrollPane getTableScroll() { return tableScroll; }

    // -------------------------------------------------------------------------
    // Bounding box highlight rendering
    // -------------------------------------------------------------------------

    /**
     * Renders the bounding box highlight for the currently hovered set.
     * Called from GameScreen.renderExtra().
     */
    public void renderHighlight(ShapeRenderer renderer, float tableY) {
        if (highlightedSetIndex < 0 || highlightedSetIndex >= setBoundingBoxes.size()) return;
        Rectangle bb = setBoundingBoxes.get(highlightedSetIndex);
        if (bb.width <= 0) return;

        // Convert tableGroup-local coords to screen coords
        float screenX = bb.x - tableScroll.getScrollX() + tableScroll.getX();
        float screenY = bb.y + tableY;

        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                           com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(0.3f, 0.6f, 1f, 0.25f);
        renderer.rect(screenX, screenY, bb.width, bb.height);
        renderer.end();

        renderer.begin(ShapeRenderer.ShapeType.Line);
        renderer.setColor(0.4f, 0.7f, 1f, 0.7f);
        renderer.rect(screenX, screenY, bb.width, bb.height);
        renderer.end();

        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    // -------------------------------------------------------------------------
    // Dispose
    // -------------------------------------------------------------------------

    /** Disposes all tile actors in the table group. */
    public void dispose() {
        if (tableGroup == null) return;
        for (Actor a : tableGroup.getChildren()) {
            if (a instanceof TileActor) ((TileActor) a).dispose();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Label createLabel(String text) {
        Label.LabelStyle style = new Label.LabelStyle();
        style.font = new BitmapFont();
        style.fontColor = Color.WHITE;
        return new Label(text, style);
    }
}
