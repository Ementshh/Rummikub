package com.rummikub.utils;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;


 //Shared pool of expensive native rendering resources.

public class ResourcePool {

    private static ResourcePool instance;

    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private ResourcePool() {}

    public static ResourcePool getInstance() {
        if (instance == null) {
            instance = new ResourcePool();
        }
        return instance;
    }


    public ShapeRenderer getShapeRenderer() {
        if (shapeRenderer == null) {
            shapeRenderer = new ShapeRenderer();
        }
        return shapeRenderer;
    }


    public BitmapFont getFont() {
        if (font == null) {
            font = new BitmapFont();
        }
        return font;
    }


    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        if (font != null) {
            font.dispose();
            font = null;
        }
    }
}
