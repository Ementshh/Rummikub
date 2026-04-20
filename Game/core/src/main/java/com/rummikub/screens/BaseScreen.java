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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.rummikub.RummikubGame;

/**
 * [TEMPLATE METHOD] — Base class for all screens.
 *
 * Subclasses must implement {@link #buildUI()} and may override the optional
 * hooks {@link #onShow()}, {@link #update(float)}, {@link #renderExtra},
 * and {@link #onDispose()}.
 *
 * The lifecycle methods (show, render, resize, dispose) are final and must
 * not be overridden.
 */
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

    /** Creates a Label using the default BitmapFont. */
    protected Label makeLabel(String text) {
        Label.LabelStyle style = new Label.LabelStyle();
        style.font = new BitmapFont();
        style.fontColor = Color.WHITE;
        return new Label(text, style);
    }

    /** Creates a TextButton with a solid dark-blue background. */
    protected TextButton makeButton(String text) {
        return new TextButton(text, makeButtonStyle(new Color(0.20f, 0.30f, 0.60f, 1f)));
    }

    /**
     * Builds a minimal TextButtonStyle backed by solid-color Pixmap textures.
     * No external skin file required.
     */
    protected TextButton.TextButtonStyle makeButtonStyle(Color buttonColor) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font      = new BitmapFont();
        style.fontColor = Color.WHITE;

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);

        pm.setColor(buttonColor);
        pm.fill();
        style.up = new TextureRegionDrawable(new Texture(pm));

        pm.setColor(buttonColor.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
        pm.fill();
        style.down = new TextureRegionDrawable(new Texture(pm));

        pm.dispose();
        return style;
    }

    /** Logs a message via Gdx.app.log. */
    protected void showMessage(String msg) {
        Gdx.app.log(getClass().getSimpleName(), msg);
    }
}
