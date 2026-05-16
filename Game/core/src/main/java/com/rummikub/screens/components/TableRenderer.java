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
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.rummikub.actors.TileActor;
import com.rummikub.factory.TileActorFactory;
import com.rummikub.network.dto.TableSetDto;
import com.rummikub.network.dto.TileDto;
import com.rummikub.state.GameStateManager;
import com.rummikub.strategy.LockedTileStrategy;
import com.rummikub.strategy.TableTileStrategy;
import com.rummikub.strategy.TileRenderStrategy;
import com.rummikub.utils.Constants;
import com.rummikub.utils.ResourcePool;
import com.rummikub.utils.TextureCache;

import java.util.ArrayList;
import java.util.List;


// Handle rendering tile sets di meja. Menggunakan {@link TextureCache} untuk solid color drawable dan  {@link ResourcePool} untuk shared bitmapfont.

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

    private int numCols = 2;

    // Pre-cached drawables and colors for slot rendering (avoid re-creation)
    private final Drawable slotBgDrawable;
    private final Drawable slotBorderDrawable;

    // Pre-cached label style (shared font, created once)
    private final Label.LabelStyle sharedLabelStyle;

    public TableRenderer(Group tableGroup, ScrollPane tableScroll,
                         GameStateManager gsm, SetValidator setValidator,
                         float tableHeight) {
        this.tableGroup = tableGroup;
        this.tableScroll = tableScroll;
        this.gsm = gsm;
        this.setValidator = setValidator;
        this.tableHeight = tableHeight;

        // Pre-cache drawables used repeatedly in rebuild cycles
        TextureCache tc = TextureCache.getInstance();
        this.slotBgDrawable = tc.getColorDrawable(new Color(0.3f, 0.3f, 0.3f, 0.15f));
        this.slotBorderDrawable = tc.getColorDrawable(new Color(0.5f, 0.5f, 0.5f, 0.3f));

        // Shared label style — one BitmapFont for all labels
        this.sharedLabelStyle = new Label.LabelStyle();
        this.sharedLabelStyle.font = ResourcePool.getInstance().getFont();
        this.sharedLabelStyle.fontColor = Color.WHITE;
    }

    // Set banyak kolom yang akan dirender
    public void setNumColumns(int numCols) {
        this.numCols = Math.max(2, numCols);
    }

    public int getNumColumns() {
        return numCols;
    }

    public void rebuild(TileDragHandler dragHandler) {
        if (tableGroup == null) return;

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

        float rowHeight = SLOT_VISUAL_HEIGHT + 20f;
        float startX = setMargin;
        float startY = tableHeight - 20f;

        //  pastikan cukup kolom untuk set
        int requiredCols = Math.max(numCols, (int) Math.ceil((double) sets.size() / GRID_ROWS));
        numCols = Math.max(2, requiredCols);

        // hitung max tiles per kolom supaya lebarnya fleksibel
        int[] maxTilesPerColumn = new int[numCols];
        for (int si = 0; si < sets.size(); si++) {
            int col = si / GRID_ROWS;
            if (col < numCols) {
                int tileCount = sets.get(si).tile_ids.size();
                maxTilesPerColumn[col] = Math.max(maxTilesPerColumn[col], tileCount);
            }
        }

        // hitung variable column widths: base 3 tiles + extension for extras
        float[] colWidths = new float[numCols];
        for (int c = 0; c < numCols; c++) {
            int extraTiles = Math.max(0, maxTilesPerColumn[c] - 3);
            int baseTiles = 3;
            colWidths[c] = Math.max(SLOT_VISUAL_WIDTH, tileW * (baseTiles + extraTiles) + 10f);
        }

        // hitung offset X untuk setiap kolom
        float[] colXOffsets = new float[numCols];
        colXOffsets[0] = startX;
        for (int c = 1; c < numCols; c++) {
            colXOffsets[c] = colXOffsets[c - 1] + colWidths[c - 1];
        }

        // Update table group size for scrolling
        float totalWidth = colXOffsets[numCols - 1] + colWidths[numCols - 1] + setMargin;
        tableGroup.setSize(totalWidth, tableHeight);

        // Render ALL visible slots (including gaps)
        int totalSlots = numCols * GRID_ROWS;
        renderAllSlots(sets.size(), totalSlots, startX, startY, colWidths, rowHeight, tileH);

        // Render existing sets (including empty ones as gaps)
        for (int si = 0; si < sets.size(); si++) {
            TableSetDto set = sets.get(si);

            int col = si / GRID_ROWS;
            int row = si % GRID_ROWS;

            float slotX = colXOffsets[col];
            float slotY = startY - (row + 1) * rowHeight + (rowHeight - tileH) / 2f;

            // Handle empty sets as gaps (still render slot but no tiles)
            if (set == null || set.isEmpty()) {
                Gdx.app.log("TableRenderer", "set " + si + " is empty/gap, at col=" + col + " row=" + row);
                setBoundingBoxes.add(new Rectangle(0, 0, 0, 0));
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

            Label setLabel = new Label(labelText, sharedLabelStyle);
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

                // Tile cuman di commit jika set udah di commit sebelumnya dan tilenya tidak di place dari rack di turn ini
                boolean isTileCommitted = !set.isNewThisTurn && !gsm.isTilePlacedThisTurn(tileId);

                TileRenderStrategy strategy = isTileCommitted
                    ? new LockedTileStrategy()   // dark border = committed, cannot return to rack
                    : new TableTileStrategy();    // cyan highlight = returnable to rack

                TileActor actor = TileActorFactory.create(dto, strategy);
                actor.setPosition(slotX + ti * tileW, slotY);
                final int setIndex = si;
                dragHandler.attachDropListener(actor, "TABLE", setIndex, isTileCommitted);
                dragHandler.attachDragMoveListener(actor, "TABLE", isTileCommitted);
                tableGroup.addActor(actor);
            }
        }

        // Add extra empty slots at the end for new sets
        addEmptySlotDropZones(sets.size(), numCols, startX, startY, colWidths, rowHeight);
    }

    // render visual indikator untuk semua grid slot visible
    private void renderAllSlots(int setCount, int totalSlots, float startX, float startY,
                                 float[] colWidths, float rowHeight, float tileH) {

        // hitung offset X untuk setiap kolom
        float[] colXOffsets = new float[colWidths.length];
        colXOffsets[0] = startX;
        for (int c = 1; c < colWidths.length; c++) {
            colXOffsets[c] = colXOffsets[c - 1] + colWidths[c - 1];
        }

        for (int slotIndex = 0; slotIndex < totalSlots; slotIndex++) {
            int col = slotIndex / GRID_ROWS;
            int row = slotIndex % GRID_ROWS;

            if (col >= colWidths.length) continue; // Safety check

            float slotX = colXOffsets[col];
            float slotY = startY - (row + 1) * rowHeight + (rowHeight - tileH) / 2f;
            float colWidth = colWidths[col];

            // Store bounding box for this slot (for hit testing)
            Rectangle slotBB = new Rectangle(
                slotX - 5f,
                slotY - 5f,
                colWidth + 10f,
                SLOT_VISUAL_HEIGHT + 10f
            );
            emptySlotBoxes.add(slotBB);

            // Render visual for ALL slots (both empty gaps and available slots)
            // Empty slots beyond current sets or existing gaps get rendered
            boolean isGap = slotIndex < setCount;
            boolean isBeyondSets = slotIndex >= setCount;

            // Always render slot visual for empty slots
            if (isBeyondSets || (isGap && gsm.getTableSets().get(slotIndex).isEmpty())) {
                // slot indicator
                Image slotBg = new Image(slotBgDrawable);
                slotBg.setBounds(slotX, slotY, colWidth, SLOT_VISUAL_HEIGHT);
                tableGroup.addActor(slotBg);

                // Border effect
                Image borderTop = new Image(slotBorderDrawable);
                borderTop.setBounds(slotX, slotY + SLOT_VISUAL_HEIGHT - 1, colWidth, 1);
                tableGroup.addActor(borderTop);

                Image borderBottom = new Image(slotBorderDrawable);
                borderBottom.setBounds(slotX, slotY, colWidth, 1);
                tableGroup.addActor(borderBottom);

                Image borderLeft = new Image(slotBorderDrawable);
                borderLeft.setBounds(slotX, slotY, 1, SLOT_VISUAL_HEIGHT);
                tableGroup.addActor(borderLeft);

                Image borderRight = new Image(slotBorderDrawable);
                borderRight.setBounds(slotX + colWidth - 1, slotY, 1, SLOT_VISUAL_HEIGHT);
                tableGroup.addActor(borderRight);

                // + sign
                Label plusLabel = new Label("+", sharedLabelStyle);
                plusLabel.setColor(new Color(0.5f, 0.5f, 0.5f, 0.5f));
                plusLabel.setPosition(slotX + colWidth / 2f - 8f,
                                       slotY + SLOT_VISUAL_HEIGHT / 2f - 10f);
                tableGroup.addActor(plusLabel);
            }
        }
    }

    /**
     * Adds drop zones for empty grid slots.
     */
    private void addEmptySlotDropZones(int setCount, int numCols, float startX, float startY,
                                        float[] colWidths, float rowHeight) {
        int totalSlots = numCols * GRID_ROWS;

        // hitung offset X untuk setiap kolom
        float[] colXOffsets = new float[colWidths.length];
        colXOffsets[0] = startX;
        for (int c = 1; c < colWidths.length; c++) {
            colXOffsets[c] = colXOffsets[c - 1] + colWidths[c - 1];
        }

        for (int slotIndex = setCount; slotIndex < totalSlots; slotIndex++) {
            int col = slotIndex / GRID_ROWS;
            int row = slotIndex % GRID_ROWS;

            if (col >= colWidths.length) continue; // Safety check

            float slotX = colXOffsets[col];
            float slotY = startY - (row + 1) * rowHeight + 10f;
            float colWidth = colWidths[col];

            Actor zone = new Actor();
            zone.setBounds(slotX, slotY, colWidth, SLOT_VISUAL_HEIGHT);
            final int targetSlotIndex = slotIndex;
            zone.addListener(new DragListener() {
                @Override
                public void dragStop(InputEvent event, float x, float y, int pointer) {
                }
            });
            tableGroup.addActor(zone);
        }
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


    public void dispose() {
        // No-op: TileActor tidak lagi memiliki native resources sendiri.
        // Semua ShapeRenderer/BitmapFont di-share via ResourcePool.
    }
}
