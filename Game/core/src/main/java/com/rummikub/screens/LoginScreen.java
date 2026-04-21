package com.rummikub.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.rummikub.RummikubGame;
import com.rummikub.network.ApiCallback;
import com.rummikub.network.GameApiFacade;
import com.rummikub.network.dto.GenericResponse;
import com.rummikub.network.dto.LoginResponse;

/**
 * Login / Register screen.
 *
 * Layout (Scene2D Table, centered):
 *   Title label
 *   Username TextField
 *   Password TextField (password mode)
 *   LOGIN button
 *   REGISTER button
 *   Error label (red, hidden by default)
 */
public class LoginScreen extends BaseScreen {

    private final GameApiFacade facade = new GameApiFacade();

    private TextField usernameField;
    private TextField passwordField;
    private TextButton loginButton;
    private TextButton registerButton;
    private Label errorLabel;

    public LoginScreen(RummikubGame game) {
        super(game);
    }

    // -------------------------------------------------------------------------
    // buildUI — Template Method hook
    // -------------------------------------------------------------------------

    @Override
    protected void buildUI() {
        // ---- Title ----
        Label titleLabel = makeLabel("RUMMIKUB ONLINE");
        titleLabel.setFontScale(2f);

        // ---- Text fields ----
        TextField.TextFieldStyle fieldStyle = buildTextFieldStyle();

        usernameField = new TextField("", fieldStyle);
        usernameField.setMessageText("Username");

        passwordField = new TextField("", fieldStyle);
        passwordField.setMessageText("Password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');

        // ---- Buttons ----
        loginButton    = makeButton("LOGIN",    new Color(0.15f, 0.45f, 0.15f, 1f));
        registerButton = makeButton("REGISTER", new Color(0.35f, 0.35f, 0.35f, 1f));

        // ---- Error label ----
        errorLabel = makeLabel("");
        errorLabel.setColor(Color.RED);
        errorLabel.setVisible(false);

        // ---- Layout ----
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(titleLabel).colspan(2).padBottom(40).row();
        table.add(makeLabel("Username:")).right().padRight(10);
        table.add(usernameField).width(300).height(40).row();
        table.add(makeLabel("Password:")).right().padRight(10).padTop(10);
        table.add(passwordField).width(300).height(40).padTop(10).row();
        table.add(loginButton).colspan(2).width(300).height(50).padTop(20).row();
        table.add(registerButton).colspan(2).width(300).height(50).padTop(10).row();
        table.add(errorLabel).colspan(2).padTop(12).row();

        stage.addActor(table);

        // ---- Listeners ----
        loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onLoginClicked();
            }
        });

        registerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onRegisterClicked();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Button handlers
    // -------------------------------------------------------------------------

    private void onLoginClicked() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Username dan password tidak boleh kosong");
            return;
        }

        setButtonsEnabled(false);

        facade.login(user, pass, new ApiCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse r) {
                if (r.success) {
                    game.setScreen(new LobbyScreen(game));
                } else {
                    showError(r.error != null ? r.error : "Login gagal");
                    setButtonsEnabled(true);
                }
            }

            @Override
            public void onFailure(String err) {
                showError("Koneksi gagal: " + err);
                setButtonsEnabled(true);
            }
        });
    }

    private void onRegisterClicked() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Username dan password tidak boleh kosong");
            return;
        }

        setButtonsEnabled(false);

        facade.register(user, pass, new ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse r) {
                if (r.success) {
                    // Auto-login after successful registration
                    facade.login(user, pass, new ApiCallback<LoginResponse>() {
                        @Override
                        public void onSuccess(LoginResponse lr) {
                            if (lr.success) {
                                game.setScreen(new LobbyScreen(game));
                            } else {
                                showError("Registrasi berhasil, silakan login");
                                setButtonsEnabled(true);
                            }
                        }

                        @Override
                        public void onFailure(String err) {
                            showError("Registrasi berhasil, silakan login");
                            setButtonsEnabled(true);
                        }
                    });
                } else {
                    showError(r.error != null ? r.error : "Registrasi gagal");
                    setButtonsEnabled(true);
                }
            }

            @Override
            public void onFailure(String err) {
                showError("Koneksi gagal: " + err);
                setButtonsEnabled(true);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void setButtonsEnabled(boolean enabled) {
        loginButton.setDisabled(!enabled);
        registerButton.setDisabled(!enabled);
    }

    /**
     * Builds a minimal TextFieldStyle backed by solid-color Pixmap textures.
     * No external skin file required.
     */
    private TextField.TextFieldStyle buildTextFieldStyle() {
        com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(
                1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);

        pm.setColor(Color.WHITE);
        pm.fill();
        com.badlogic.gdx.graphics.g2d.TextureRegion bgRegion =
                new com.badlogic.gdx.graphics.g2d.TextureRegion(
                        new com.badlogic.gdx.graphics.Texture(pm));

        pm.setColor(new Color(0.8f, 0.9f, 1f, 1f));
        pm.fill();
        com.badlogic.gdx.graphics.g2d.TextureRegion focusRegion =
                new com.badlogic.gdx.graphics.g2d.TextureRegion(
                        new com.badlogic.gdx.graphics.Texture(pm));

        pm.dispose();

        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font            = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        style.fontColor       = Color.BLACK;
        style.messageFontColor = Color.GRAY;
        style.background      = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(bgRegion);
        style.focusedBackground = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(focusRegion);
        style.cursor          = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(bgRegion);
        return style;
    }
}
