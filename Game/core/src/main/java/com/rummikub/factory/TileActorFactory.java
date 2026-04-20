package com.rummikub.factory;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.rummikub.actors.TileActor;
import com.rummikub.network.dto.TileDto;
import com.rummikub.strategy.TileRenderStrategy;
import com.rummikub.utils.ColorMapper;

/**
 * [FACTORY METHOD] — Centralises TileActor creation.
 *
 * Callers never construct TileActor directly; they go through one of the
 * factory methods here so that joker/normal/drop-slot logic stays in one place.
 */
public class TileActorFactory {

    private TileActorFactory() {}

    // -------------------------------------------------------------------------
    // Public factory methods
    // -------------------------------------------------------------------------

    /**
     * Main entry point — dispatches to the appropriate sub-factory based on
     * whether the tile is a joker.
     */
    public static TileActor create(TileDto tile, TileRenderStrategy strategy) {
        if (tile.isJoker) {
            return createJoker(tile, strategy);
        }
        return createNormal(tile, strategy);
    }

    /**
     * Creates a standard numbered tile.
     * Text is black on yellow tiles for legibility; white on all others.
     */
    protected static TileActor createNormal(TileDto tile, TileRenderStrategy strategy) {
        Color bg   = ColorMapper.toLibGDX(tile.color);
        Color text = "YELLOW".equalsIgnoreCase(tile.color) ? Color.BLACK : Color.WHITE;
        return new TileActor(tile, bg, text, String.valueOf(tile.number), strategy);
    }

    /**
     * Creates a joker tile with a distinctive purple background.
     */
    protected static TileActor createJoker(TileDto tile, TileRenderStrategy strategy) {
        return new TileActor(
                tile,
                new Color(0.60f, 0.10f, 0.80f, 1f),
                Color.WHITE,
                "J",
                strategy
        );
    }

    /**
     * Creates a lightweight placeholder actor that draws a dashed-style border,
     * used as a visual drop slot on the table.
     */
    public static Actor createDropSlot(float width, float height) {
        return new Actor() {

            private final ShapeRenderer sr = new ShapeRenderer();

            {
                setSize(width, height);
            }

            @Override
            public void draw(Batch batch, float parentAlpha) {
                float x = getX();
                float y = getY();
                float w = getWidth();
                float h = getHeight();

                batch.end();

                sr.setProjectionMatrix(batch.getProjectionMatrix());
                sr.begin(ShapeRenderer.ShapeType.Line);
                sr.setColor(new Color(1f, 1f, 1f, 0.4f));

                // Draw four sides as short dashed segments
                float dash = 6f;
                float gap  = 4f;

                // Bottom edge
                for (float px = x; px < x + w; px += dash + gap) {
                    sr.line(px, y, Math.min(px + dash, x + w), y);
                }
                // Top edge
                for (float px = x; px < x + w; px += dash + gap) {
                    sr.line(px, y + h, Math.min(px + dash, x + w), y + h);
                }
                // Left edge
                for (float py = y; py < y + h; py += dash + gap) {
                    sr.line(x, py, x, Math.min(py + dash, y + h));
                }
                // Right edge
                for (float py = y; py < y + h; py += dash + gap) {
                    sr.line(x + w, py, x + w, Math.min(py + dash, y + h));
                }

                sr.end();
                batch.begin();
            }
        };
    }
}
