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

    private com.badlogic.gdx.graphics.Texture bgLogin;
    private com.badlogic.gdx.graphics.Texture bgLobby;
    private com.badlogic.gdx.graphics.Texture bgWaitingRoom;
    private com.badlogic.gdx.graphics.Texture bgGameOver;
    private com.badlogic.gdx.graphics.g2d.BitmapFont customFont;

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

    public com.badlogic.gdx.graphics.Texture getBgLogin() {
        if (bgLogin == null) {
            bgLogin = new com.badlogic.gdx.graphics.Texture("bg_loginscreen.jpg");
        }
        return bgLogin;
    }

    public com.badlogic.gdx.graphics.Texture getBgLobby() {
        if (bgLobby == null) {
            bgLobby = new com.badlogic.gdx.graphics.Texture("bg_lobby.jpg");
        }
        return bgLobby;
    }

    public com.badlogic.gdx.graphics.Texture getBgWaitingRoom() {
        if (bgWaitingRoom == null) {
            bgWaitingRoom = new com.badlogic.gdx.graphics.Texture("bg_waitingroom.jpg");
        }
        return bgWaitingRoom;
    }

    public com.badlogic.gdx.graphics.Texture getBgGameOver() {
        if (bgGameOver == null) {
            bgGameOver = new com.badlogic.gdx.graphics.Texture("bg_gameover.jpg");
        }
        return bgGameOver;
    }

    public com.badlogic.gdx.graphics.g2d.BitmapFont getCustomFont() {
        if (customFont == null) {
            customFont = new com.badlogic.gdx.graphics.g2d.BitmapFont(com.badlogic.gdx.Gdx.files.internal("myfont.fnt"));
        }
        return customFont;
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
        if (bgLogin != null) {
            bgLogin.dispose();
            bgLogin = null;
        }
        if (bgLobby != null) {
            bgLobby.dispose();
            bgLobby = null;
        }
        if (bgWaitingRoom != null) {
            bgWaitingRoom.dispose();
            bgWaitingRoom = null;
        }
        if (bgGameOver != null) {
            bgGameOver.dispose();
            bgGameOver = null;
        }
        if (customFont != null) {
            customFont.dispose();
            customFont = null;
        }
    }
}
