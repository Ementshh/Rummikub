package com.rummikub.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;


 //Shared pool of expensive native rendering resources.

public class ResourcePool {

    private static ResourcePool instance;

    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private TextureAtlas tileAtlas;
    private TextureAtlas uiAtlas;

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

    public TextureAtlas getTileAtlas() {
        if (tileAtlas == null) {
            tileAtlas = new TextureAtlas("tiles.atlas");
        }
        return tileAtlas;
    }

    public TextureAtlas getUiAtlas() {
        if (uiAtlas == null) {
            uiAtlas = new TextureAtlas(Gdx.files.internal("ui.atlas"));
        }
        return uiAtlas;
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
        if (tileAtlas != null) {
            tileAtlas.dispose();
            tileAtlas = null;
        }
        if (uiAtlas != null) {
            uiAtlas.dispose();
            uiAtlas = null;
        }
    }
}
