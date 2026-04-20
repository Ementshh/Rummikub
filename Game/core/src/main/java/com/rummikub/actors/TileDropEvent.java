package com.rummikub.actors;

import com.badlogic.gdx.scenes.scene2d.Event;

/**
 * Custom LibGDX event fired when a TileActor is dropped after dragging.
 */
public class TileDropEvent extends Event {

    public final TileActor tile;
    public final float dropX;
    public final float dropY;

    public TileDropEvent(TileActor tile, float dropX, float dropY) {
        this.tile = tile;
        this.dropX = dropX;
        this.dropY = dropY;
    }
}
