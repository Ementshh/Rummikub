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
    private com.badlogic.gdx.graphics.g2d.BitmapFont inputFont;

    private com.badlogic.gdx.audio.Sound clickSound;
    private com.badlogic.gdx.audio.Sound placeSound;

    private com.badlogic.gdx.audio.Music bgmMenu;
    private com.badlogic.gdx.audio.Music bgmGame;
    private com.badlogic.gdx.audio.Sound sfxWin;
    private com.badlogic.gdx.audio.Sound sfxLose;
    private com.badlogic.gdx.audio.Music currentBgm;
    private boolean isMuted = false;

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

    public com.badlogic.gdx.graphics.g2d.BitmapFont getInputFont() {
        if (inputFont == null) {
            if (!com.badlogic.gdx.Gdx.files.internal("myfont_input_0.png").exists()) {
                com.badlogic.gdx.Gdx.app.error("ResourcePool", "myfont_input_0.png not found!");
            } else {
                com.badlogic.gdx.Gdx.app.log("ResourcePool", "myfont_input_0.png found successfully.");
            }
            inputFont = new com.badlogic.gdx.graphics.g2d.BitmapFont(com.badlogic.gdx.Gdx.files.internal("myfont_input.fnt"));
        }
        return inputFont;
    }

    public com.badlogic.gdx.audio.Sound getClickSound() {
        if (clickSound == null) {
            clickSound = Gdx.audio.newSound(Gdx.files.internal("sfx_click.ogg"));
        }
        return clickSound;
    }

    public com.badlogic.gdx.audio.Sound getPlaceSound() {
        if (placeSound == null) {
            placeSound = Gdx.audio.newSound(Gdx.files.internal("sfx_place.ogg"));
        }
        return placeSound;
    }

    public com.badlogic.gdx.audio.Music getBgmMenu() {
        if (bgmMenu == null) {
            bgmMenu = Gdx.audio.newMusic(Gdx.files.internal("bgm_menu.ogg"));
            bgmMenu.setLooping(true);
        }
        return bgmMenu;
    }

    public com.badlogic.gdx.audio.Music getBgmGame() {
        if (bgmGame == null) {
            bgmGame = Gdx.audio.newMusic(Gdx.files.internal("bgm_game.ogg"));
            bgmGame.setLooping(true);
        }
        return bgmGame;
    }

    public com.badlogic.gdx.audio.Sound getSfxWin() {
        if (sfxWin == null) {
            sfxWin = Gdx.audio.newSound(Gdx.files.internal("sfx_win.ogg"));
        }
        return sfxWin;
    }

    public com.badlogic.gdx.audio.Sound getSfxLose() {
        if (sfxLose == null) {
            sfxLose = Gdx.audio.newSound(Gdx.files.internal("sfx_lose.ogg"));
        }
        return sfxLose;
    }

    public void playBgm(com.badlogic.gdx.audio.Music newBgm) {
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
        if (inputFont != null) {
            inputFont.dispose();
            inputFont = null;
        }
        if (clickSound != null) {
            clickSound.dispose();
            clickSound = null;
        }
        if (placeSound != null) {
            placeSound.dispose();
            placeSound = null;
        }
        if (bgmMenu != null) {
            bgmMenu.dispose();
            bgmMenu = null;
        }
        if (bgmGame != null) {
            bgmGame.dispose();
            bgmGame = null;
        }
        if (sfxWin != null) {
            sfxWin.dispose();
            sfxWin = null;
        }
        if (sfxLose != null) {
            sfxLose.dispose();
            sfxLose = null;
        }
        currentBgm = null;
    }
}
