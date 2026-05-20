package com.rummikub.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.rummikub.RummikubGame;
import com.rummikub.utils.ResourcePool;
import com.rummikub.utils.TextureCache;


// [TEMPLATE METHOD] — Base class for all screens.

// Subclasses must implement {@link #buildUI()} and may override the optional
// hooks {@link #onShow()}, {@link #update(float)}, {@link #renderExtra},
// and {@link #onDispose()}.

//  The lifecycle methods (show, render, resize, dispose) are final and must
// not be overridden.

// menggunakan {@link ResourcePool} untuk shared BitmapFont
// dam {@link TextureCache} untuk solid-color drawables,


public abstract class BaseScreen implements Screen {

    protected final RummikubGame game;
    protected Stage stage;
    protected SpriteBatch batch;
    protected ShapeRenderer sr;

    public BaseScreen(RummikubGame game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.sr    = new ShapeRenderer();
        this.stage = new Stage(new ScreenViewport(), batch);
        Gdx.input.setInputProcessor(stage);
    }

    // -------------------------------------------------------------------------
    // Lifecycle — FINAL (do not override in subclasses)
    // -------------------------------------------------------------------------

    @Override
    public final void show() {
        buildUI();
        onShow();
    }

    @Override
    public final void render(float delta) {
        clearScreen();
        update(delta);
        stage.act(delta);
        stage.draw();
        renderExtra(batch, sr);
    }

    @Override
    public final void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public final void dispose() {
        stage.dispose();
        batch.dispose();
        sr.dispose();
        onDispose();
    }

    // -------------------------------------------------------------------------
    // Abstract hooks — must be implemented by subclasses
    // -------------------------------------------------------------------------

    /** Build all Scene2D actors and add them to {@link #stage}. */
    protected abstract void buildUI();

    // -------------------------------------------------------------------------
    // Optional hooks — override as needed
    // -------------------------------------------------------------------------

    protected void onShow() {}
    protected void update(float delta) {}
    protected void renderExtra(SpriteBatch batch, ShapeRenderer sr) {}
    protected void onDispose() {}

    // -------------------------------------------------------------------------
    // Unused Screen interface methods
    // -------------------------------------------------------------------------

    @Override public void pause()  {}
    @Override public void resume() {}
    @Override public void hide()   {}

    // -------------------------------------------------------------------------
    // Helpers available to subclasses
    // -------------------------------------------------------------------------

    /** Clears the screen with the standard dark-green background. */
    private void clearScreen() {
        Gdx.gl.glClearColor(0.13f, 0.18f, 0.13f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }


    // Buat label menggunakan shared BitmapFont dari ResourcePool
    protected Label makeLabel(String text) {
        Label.LabelStyle style = new Label.LabelStyle();
        style.font = ResourcePool.getInstance().getFont();
        style.fontColor = Color.WHITE;
        return new Label(text, style);
    }

    protected TextButton makeButton(String text) {
        return new TextButton(text, makeButtonStyle(new Color(0.20f, 0.30f, 0.60f, 1f)));
    }

    protected TextButton makeButton(String text, Color color) {
        return new TextButton(text, makeButtonStyle(color));
    }

    protected Button makeImageButton(String regionPrefix) {
        TextureAtlas atlas = ResourcePool.getInstance().getUiAtlas();
        TextureRegionDrawable upDrawable = new TextureRegionDrawable(atlas.findRegion(regionPrefix + "_up"));
        TextureRegionDrawable downDrawable = new TextureRegionDrawable(atlas.findRegion(regionPrefix + "_down"));

        Button.ButtonStyle style = new Button.ButtonStyle();
        style.up = upDrawable;
        style.down = downDrawable;

        // Create a darkened/greyed-out version of the up texture for the disabled state
        TextureRegionDrawable disabledDrawable = 
            new TextureRegionDrawable(upDrawable.getRegion());
        // Tint it dark grey (R: 0.4, G: 0.4, B: 0.4, Alpha: 1)
        disabledDrawable.tint(new Color(0.4f, 0.4f, 0.4f, 1f));

        style.disabled = disabledDrawable;

        return new Button(style);
    }

    // Buat button style. Menggunakan shared font dari ResourcePool dan TextureCache
    protected TextButton.TextButtonStyle makeButtonStyle(Color buttonColor) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font      = ResourcePool.getInstance().getFont();
        style.fontColor = Color.WHITE;

        TextureCache tc = TextureCache.getInstance();
        style.up = tc.getColorDrawable(buttonColor);
        style.down = tc.getColorDrawable(buttonColor.cpy().mul(0.8f, 0.8f, 0.8f, 1f));

        return style;
    }

    // Buat solid-color Drawable yang di-cache di TextureCache
    protected Drawable makeColorDrawable(Color color) {
        return TextureCache.getInstance().getColorDrawable(color);
    }

    // Logs 
    protected void showMessage(String msg) {
        Gdx.app.log(getClass().getSimpleName(), msg);
    }
}
