package com.rummikub.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.rummikub.RummikubGame;
import com.rummikub.network.ApiCallback;
import com.rummikub.network.GameApiFacade;
import com.rummikub.network.NetworkManager;
import com.rummikub.network.dto.GameResponse;
import com.rummikub.network.dto.GenericResponse;

/**
 * Lobby screen — shown after a successful login.
 *
 * Layout (Scene2D Table, centered):
 *   Title "LOBBY"
 *   Welcome label with username
 *   "BUAT GAME BARU" button
 *   Separator label
 *   Game-ID TextField + JOIN button
 *   Error label (red, hidden by default)
 *   LOGOUT button (bottom-left)
 */
public class LobbyScreen extends BaseScreen {

    private final GameApiFacade facade = new GameApiFacade();

    private TextField gameIdField;
    private Label errorLabel;

    public LobbyScreen(RummikubGame game) {
        super(game);
    }

    // -------------------------------------------------------------------------
    // buildUI — Template Method hook
    // -------------------------------------------------------------------------

    @Override
    protected void buildUI() {
        String username = NetworkManager.getInstance().getCurrentUsername();
        if (username == null) username = "Pemain";

        // ---- Labels ----
        Label titleLabel   = makeLabel("LOBBY");
        titleLabel.setFontScale(2f);

        Label welcomeLabel = makeLabel("Selamat datang, " + username + "!");

        Label orLabel      = makeLabel("Atau masuk ke game");

        errorLabel = makeLabel("");
        errorLabel.setColor(Color.RED);
        errorLabel.setVisible(false);

        // ---- Buttons ----
        TextButton createButton  = makeButton("BUAT GAME BARU", new Color(0.15f, 0.45f, 0.15f, 1f));
        TextButton joinButton    = makeButton("JOIN",           new Color(0.20f, 0.40f, 0.70f, 1f));
        TextButton logoutButton  = makeButton("LOGOUT",         new Color(0.50f, 0.15f, 0.15f, 1f));

        // ---- Game-ID text field ----
        gameIdField = buildTextField("Game ID");

        // ---- Main table (centered) ----
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(titleLabel).colspan(2).padBottom(20).row();
        table.add(welcomeLabel).colspan(2).padBottom(30).row();
        table.add(createButton).colspan(2).width(300).height(50).padBottom(30).row();
        table.add(orLabel).colspan(2).padBottom(20).row();
        table.add(gameIdField).width(220).height(40).padRight(8);
        table.add(joinButton).width(80).height(40).row();
        table.add(errorLabel).colspan(2).padTop(12).row();

        stage.addActor(table);

        // ---- Logout button anchored bottom-left ----
        Table bottomBar = new Table();
        bottomBar.setFillParent(true);
        bottomBar.bottom().left().pad(20);
        bottomBar.add(logoutButton).width(120).height(40);
        stage.addActor(bottomBar);

        // ---- Listeners ----
        createButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onCreateGameClicked();
            }
        });

        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onJoinGameClicked();
            }
        });

        logoutButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                NetworkManager.getInstance().clearAuth();
                game.setScreen(new LoginScreen(game));
            }
        });
    }

    // -------------------------------------------------------------------------
    // Button handlers
    // -------------------------------------------------------------------------

    private void onCreateGameClicked() {
        hideError();
        facade.createGame(new ApiCallback<GameResponse>() {
            @Override
            public void onSuccess(GameResponse r) {
                if (r.success && r.data != null) {
                    String gameId = r.data.id;
                    facade.joinGame(gameId, new ApiCallback<GenericResponse>() {
                        @Override
                        public void onSuccess(GenericResponse jr) {
                            if (jr.success) {
                                game.setScreen(new WaitingRoomScreen(game, gameId, true));
                            } else {
                                showError(jr.error != null ? jr.error : "Gagal bergabung ke game");
                            }
                        }

                        @Override
                        public void onFailure(String err) {
                            showError("Gagal bergabung: " + err);
                        }
                    });
                } else {
                    showError(r.error != null ? r.error : "Gagal membuat game");
                }
            }

            @Override
            public void onFailure(String err) {
                showError("Gagal membuat game: " + err);
            }
        });
    }

    private void onJoinGameClicked() {
        String gameId = gameIdField.getText().trim();
        if (gameId.isEmpty()) {
            showError("Masukkan Game ID terlebih dahulu");
            return;
        }
        hideError();
        facade.joinGame(gameId, new ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse r) {
                if (r.success) {
                    game.setScreen(new WaitingRoomScreen(game, gameId, false));
                } else {
                    showError(r.error != null ? r.error : "Gagal bergabung ke game");
                }
            }

            @Override
            public void onFailure(String err) {
                showError("Gagal bergabung: " + err);
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

    private void hideError() {
        errorLabel.setVisible(false);
    }

    private TextField buildTextField(String placeholder) {
        com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(
                1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        com.badlogic.gdx.graphics.Texture tex = new com.badlogic.gdx.graphics.Texture(pm);
        pm.dispose();

        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font             = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        style.fontColor        = Color.BLACK;
        style.messageFontColor = Color.GRAY;
        style.background       = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(tex));
        style.cursor           = style.background;

        TextField field = new TextField("", style);
        field.setMessageText(placeholder);
        return field;
    }
}
