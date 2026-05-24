package com.rummikub.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;

// Shared pool of expensive native rendering resources.
public class ResourcePool {

    private static ResourcePool instance;

    public final AssetManager assetManager;

    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Music currentBgm;
    private boolean isMuted = false;

    private ResourcePool() {
        // Explicitly use InternalFileHandleResolver to support asset remapping in GWT
        this.assetManager = new AssetManager(new InternalFileHandleResolver());
        loadAssets();
        this.assetManager.finishLoading(); // Block until loaded
    }

    private void loadAssets() {
        assetManager.load("tiles.atlas", TextureAtlas.class);
        assetManager.load("ui.atlas", TextureAtlas.class);
        assetManager.load("bg_loginscreen.jpg", Texture.class);
        assetManager.load("bg_lobby.jpg", Texture.class);
        assetManager.load("bg_waitingroom.jpg", Texture.class);
        assetManager.load("bg_gameover.jpg", Texture.class);
        assetManager.load("myfont.fnt", BitmapFont.class);
        assetManager.load("myfont_input.fnt", BitmapFont.class);
        assetManager.load("sfx_click.ogg", Sound.class);
        assetManager.load("sfx_place.ogg", Sound.class);
        assetManager.load("bgm_menu.ogg", Music.class);
        assetManager.load("bgm_game.ogg", Music.class);
        assetManager.load("sfx_win.ogg", Sound.class);
        assetManager.load("sfx_lose.ogg", Sound.class);
    }

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
        return assetManager.get("tiles.atlas", TextureAtlas.class);
    }

    public TextureAtlas getUiAtlas() {
        return assetManager.get("ui.atlas", TextureAtlas.class);
    }

    public Texture getBgLogin() {
        return assetManager.get("bg_loginscreen.jpg", Texture.class);
    }

    public Texture getBgLobby() {
        return assetManager.get("bg_lobby.jpg", Texture.class);
    }

    public Texture getBgWaitingRoom() {
        return assetManager.get("bg_waitingroom.jpg", Texture.class);
    }

    public Texture getBgGameOver() {
        return assetManager.get("bg_gameover.jpg", Texture.class);
    }

    public BitmapFont getCustomFont() {
        return assetManager.get("myfont.fnt", BitmapFont.class);
    }

    public BitmapFont getInputFont() {
        return assetManager.get("myfont_input.fnt", BitmapFont.class);
    }

    public Sound getClickSound() {
        return assetManager.get("sfx_click.ogg", Sound.class);
    }

    public Sound getPlaceSound() {
        return assetManager.get("sfx_place.ogg", Sound.class);
    }

    public Music getBgmMenu() {
        Music m = assetManager.get("bgm_menu.ogg", Music.class);
        m.setLooping(true);
        return m;
    }

    public Music getBgmGame() {
        Music m = assetManager.get("bgm_game.ogg", Music.class);
        m.setLooping(true);
        return m;
    }

    public Sound getSfxWin() {
        return assetManager.get("sfx_win.ogg", Sound.class);
    }

    public Sound getSfxLose() {
        return assetManager.get("sfx_lose.ogg", Sound.class);
    }

    public void playBgm(Music newBgm) {
        if (currentBgm == newBgm) {
            return;
        }
        if (currentBgm != null) {
            currentBgm.stop();
        }
        currentBgm = newBgm;
        if (currentBgm != null) {
            currentBgm.setVolume(isMuted ? 0f : 1f);
            currentBgm.play();
        }
    }

    public void stopBgm() {
        if (currentBgm != null) {
            currentBgm.stop();
            currentBgm = null;
        }
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (currentBgm != null) {
            currentBgm.setVolume(isMuted ? 0f : 1f);
        }
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
        assetManager.dispose();
        currentBgm = null;
    }
}
