package com.rummikub.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.HashMap;
import java.util.Map;



public class TextureCache {

    private static TextureCache instance;

    private final Map<Integer, Texture> textureMap = new HashMap<>();

    private final Map<Integer, Drawable> drawableMap = new HashMap<>();

    private TextureCache() {}

    public static TextureCache getInstance() {
        if (instance == null) {
            instance = new TextureCache();
        }
        return instance;
    }


    public Drawable getColorDrawable(Color color) {
        int key = Color.rgba8888(color);

        Drawable cached = drawableMap.get(key);
        if (cached != null) return cached;

        // Create and cache the Texture + Drawable
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();

        textureMap.put(key, tex);

        Drawable drawable = new TextureRegionDrawable(new TextureRegion(tex));
        drawableMap.put(key, drawable);

        return drawable;
    }


    public void dispose() {
        for (Texture tex : textureMap.values()) {
            tex.dispose();
        }
        textureMap.clear();
        drawableMap.clear();
    }
}
