package com.rummikub.screens.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
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
    private static final int GRID_ROWS = 4;
    private static final float SLOT_VISUAL_WIDTH = 70f;
    private static final float SLOT_VISUAL_HEIGHT = 100f;

    private final Group tableGroup;
    private final ScrollPane tableScroll;
    private final GameStateManager gsm;
    private final SetValidator setValidator;
    private final float tableHeight;

    private final List<Rectangle> setBoundingBoxes = new ArrayList<>();
    private final List<Rectangle> emptySlotBoxes = new ArrayList<>();
    private int highlightedSetIndex = -1;
    private int highlightedSlotIndex = -1;

    public TableRenderer(Group tableGroup, ScrollPane tableScroll,
                         GameStateManager gsm, SetValidator setValidator,
                         float tableHeight) {
        this.tableGroup = tableGroup;
        this.tableScroll = tableScroll;
        this.gsm = gsm;
        this.setValidator = setValidator;
        this.tableHeight = tableHeight;
    }

    public void rebuild(TileDragHandler dragHandler) {
        if (tableGroup == null) return;

        // Dispose old actors
        for (Actor a : tableGroup.getChildren()) {
            if (a instanceof TileActor) ((TileActor) a).dispose();
        }
        tableGroup.clearChildren();
        setBoundingBoxes.clear();
        emptySlotBoxes.clear();
        highlightedSetIndex = -1;
        highlightedSlotIndex = -1;

        List<TableSetDto> sets = gsm.getTableSets();
        float setMargin = 15f;
        float tileW = Constants.TILE_WIDTH * 0.85f + 2f;
        float tileH = Constants.TILE_HEIGHT * 0.85f;

        // Calculate grid dimensions
        float colWidth = Math.max(SLOT_VISUAL_WIDTH, tileW * 4 + 10f);
        float rowHeight = SLOT_VISUAL_HEIGHT + 20f;
        float startX = setMargin;
        float startY = tableHeight - 20f;

        // Calculate how many columns needed (4 rows per column)
        int numCols = Math.max(2, (sets.size() + GRID_ROWS - 1) / GRID_ROWS + 1);

        // Update table group size for scrolling
        float totalWidth = startX + numCols * colWidth + setMargin;
        tableGroup.setSize(totalWidth, tableHeight);

        // Render empty slot indicators first (so they appear behind tiles)
        renderEmptySlots(sets.size(), numCols, startX, startY, colWidth, rowHeight, tileH);

        // Render existing sets
        for (int si = 0; si < sets.size(); si++) {
            TableSetDto set = sets.get(si);

            int col = si / GRID_ROWS;
            int row = si % GRID_ROWS;

            float slotX = startX + col * colWidth;
            float slotY = startY - (row + 1) * rowHeight + (rowHeight - tileH) / 2f;

            if (set.tile_ids == null || set.tile_ids.isEmpty()) {
                Gdx.app.log("TableRenderer", "set " + si + " is empty, at col=" + col + " row=" + row);
                setBoundingBoxes.add(new Rectangle(slotX, slotY, 0, 0));
                continue;
            }

            int tileCount = set.tile_ids.size();
            float setPixelW = tileCount * tileW;

            // Compute bounding box for this set (in tableGroup local coords)
            Rectangle bb = new Rectangle(
                slotX - BB_PADDING,
                slotY - BB_PADDING,
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
            setLabel.setPosition(slotX, slotY + tileH + 6);
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
                actor.setPosition(slotX + ti * tileW, slotY);
                final int setIndex = si;
                boolean isCommitted = !set.isNewThisTurn;
                dragHandler.attachDropListener(actor, "TABLE", setIndex, isCommitted);
                dragHandler.attachDragMoveListener(actor, "TABLE", isCommitted);
                tableGroup.addActor(actor);
            }
        }

        // Add extra empty slots at the end for new sets
        addEmptySlotDropZones(sets.size(), numCols, startX, startY, colWidth, rowHeight);
    }

    //Renders visual indicators for empty grid slots.
    private void renderEmptySlots(int setCount, int numCols, float startX, float startY,
                                   float colWidth, float rowHeight, float tileH) {
        int totalSlots = numCols * GRID_ROWS;

        for (int slotIndex = 0; slotIndex < totalSlots; slotIndex++) {
            int col = slotIndex / GRID_ROWS;
            int row = slotIndex % GRID_ROWS;

            float slotX = startX + col * colWidth;
            float slotY = startY - (row + 1) * rowHeight + (rowHeight - tileH) / 2f;

            // Store bounding box for this slot (for hit testing)
            Rectangle slotBB = new Rectangle(
                slotX - 5f,
                slotY - 5f,
                SLOT_VISUAL_WIDTH + 10f,
                SLOT_VISUAL_HEIGHT + 10f
            );
            emptySlotBoxes.add(slotBB);

            // Only render visual for empty slots (beyond current sets)
            if (slotIndex >= setCount) {
                // Semi-transparent slot indicator
                Image slotBg = new Image(makeColorDrawable(new Color(0.3f, 0.3f, 0.3f, 0.15f)));
                slotBg.setBounds(slotX, slotY, SLOT_VISUAL_WIDTH, SLOT_VISUAL_HEIGHT);
                tableGroup.addActor(slotBg);

                // Dashed border effect (using thin lines)
                Image borderTop = new Image(makeColorDrawable(new Color(0.5f, 0.5f, 0.5f, 0.3f)));
                borderTop.setBounds(slotX, slotY + SLOT_VISUAL_HEIGHT - 1, SLOT_VISUAL_WIDTH, 1);
                tableGroup.addActor(borderTop);

                Image borderBottom = new Image(makeColorDrawable(new Color(0.5f, 0.5f, 0.5f, 0.3f)));
                borderBottom.setBounds(slotX, slotY, SLOT_VISUAL_WIDTH, 1);
                tableGroup.addActor(borderBottom);

                Image borderLeft = new Image(makeColorDrawable(new Color(0.5f, 0.5f, 0.5f, 0.3f)));
                borderLeft.setBounds(slotX, slotY, 1, SLOT_VISUAL_HEIGHT);
                tableGroup.addActor(borderLeft);

                Image borderRight = new Image(makeColorDrawable(new Color(0.5f, 0.5f, 0.5f, 0.3f)));
                borderRight.setBounds(slotX + SLOT_VISUAL_WIDTH - 1, slotY, 1, SLOT_VISUAL_HEIGHT);
                tableGroup.addActor(borderRight);

                // Add "+" indicator in center
                Label plusLabel = createLabel("+");
                plusLabel.setFontScale(1.2f);
                plusLabel.setColor(new Color(0.5f, 0.5f, 0.5f, 0.4f));
                plusLabel.setPosition(slotX + SLOT_VISUAL_WIDTH / 2f - 8f,
                                       slotY + SLOT_VISUAL_HEIGHT / 2f - 10f);
                tableGroup.addActor(plusLabel);
            }
        }
    }

    /**
     * Adds drop zones for empty grid slots.
     */
    private void addEmptySlotDropZones(int setCount, int numCols, float startX, float startY,
                                        float colWidth, float rowHeight) {
        int totalSlots = numCols * GRID_ROWS;

        for (int slotIndex = setCount; slotIndex < totalSlots; slotIndex++) {
            int col = slotIndex / GRID_ROWS;
            int row = slotIndex % GRID_ROWS;

            float slotX = startX + col * colWidth;
            float slotY = startY - (row + 1) * rowHeight + 10f;

            Actor zone = new Actor();
            zone.setBounds(slotX, slotY, SLOT_VISUAL_WIDTH, SLOT_VISUAL_HEIGHT);
            final int targetSlotIndex = slotIndex;
            zone.addListener(new DragListener() {
                @Override
                public void dragStop(InputEvent event, float x, float y, int pointer) {
                }
            });
            tableGroup.addActor(zone);
        }
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable makeColorDrawable(Color color) {
        com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(1, 1,
            com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        com.badlogic.gdx.graphics.Texture tex = new com.badlogic.gdx.graphics.Texture(pm);
        pm.dispose();
        return new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(tex));
    }

    // Bounding box API


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

    /**
     * Returns the index of the empty slot whose bounding box contains the given
     * tableGroup-local coordinates, or -1 if no match.
     */
    public int findEmptySlotIndexAt(float localX, float localY) {
        for (int i = 0; i < emptySlotBoxes.size(); i++) {
            Rectangle bb = emptySlotBoxes.get(i);
            if (bb.contains(localX, localY)) {
                return i;
            }
        }
        return -1;
    }

    public int getHighlightedSetIndex() { return highlightedSetIndex; }
    public void setHighlightedSetIndex(int idx) { this.highlightedSetIndex = idx; }
    public int getHighlightedSlotIndex() { return highlightedSlotIndex; }
    public void setHighlightedSlotIndex(int idx) { this.highlightedSlotIndex = idx; }
    public ScrollPane getTableScroll() { return tableScroll; }


    // Bounding box highlight rendering

    /**
     * Renders the bounding box highlight for the currently hovered set or empty slot.
     * Called from GameScreen.renderExtra().
     */
    public void renderHighlight(ShapeRenderer renderer, float tableY) {
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                           com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Highlight existing set
        if (highlightedSetIndex >= 0 && highlightedSetIndex < setBoundingBoxes.size()) {
            Rectangle bb = setBoundingBoxes.get(highlightedSetIndex);
            if (bb.width > 0) {
                float screenX = bb.x - tableScroll.getScrollX() + tableScroll.getX();
                float screenY = bb.y + tableY;

                renderer.begin(ShapeRenderer.ShapeType.Filled);
                renderer.setColor(0.3f, 0.6f, 1f, 0.25f);
                renderer.rect(screenX, screenY, bb.width, bb.height);
                renderer.end();

                renderer.begin(ShapeRenderer.ShapeType.Line);
                renderer.setColor(0.4f, 0.7f, 1f, 0.7f);
                renderer.rect(screenX, screenY, bb.width, bb.height);
                renderer.end();
            }
        }

        // Highlight empty slot
        if (highlightedSlotIndex >= 0 && highlightedSlotIndex < emptySlotBoxes.size()) {
            Rectangle bb = emptySlotBoxes.get(highlightedSlotIndex);
            float screenX = bb.x - tableScroll.getScrollX() + tableScroll.getX();
            float screenY = bb.y + tableY;

            renderer.begin(ShapeRenderer.ShapeType.Filled);
            renderer.setColor(0.4f, 0.8f, 0.4f, 0.35f);
            renderer.rect(screenX, screenY, bb.width, bb.height);
            renderer.end();

            renderer.begin(ShapeRenderer.ShapeType.Line);
            renderer.setColor(0.5f, 0.9f, 0.5f, 0.8f);
            renderer.rect(screenX, screenY, bb.width, bb.height);
            renderer.end();
        }

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
