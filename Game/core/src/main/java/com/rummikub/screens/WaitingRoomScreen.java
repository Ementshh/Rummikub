package com.rummikub.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.rummikub.RummikubGame;
import com.rummikub.network.ApiCallback;
import com.rummikub.network.GameApiFacade;
import com.rummikub.network.dto.GameStateResponse;
import com.rummikub.network.dto.GenericResponse;
import com.rummikub.network.dto.ParticipantDto;
import com.rummikub.state.GameStateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Waiting room screen — shown after creating or joining a game.
 *
 * Polls the server every 3 seconds to refresh the player list and detect
 * when the game transitions to IN_PROGRESS.
 *
 * Layout (Scene2D Table, centered):
 *   Title "RUANG TUNGGU"
 *   Game ID label (for sharing)
 *   Player list label (multi-line, updated on each poll)
 *   Status label
 *   "MULAI GAME" button (host only, enabled when >= 2 players)
 *   "KEMBALI" button
 */
public class WaitingRoomScreen extends BaseScreen {

    private final String gameId;
    private final boolean isHost;
    private final GameApiFacade facade = new GameApiFacade();

    private float pollTimer = 0f;
    private final List<String> playerNames = new ArrayList<>();

    private Label playerListLabel;
    private Label statusLabel;
    private TextButton startButton;

    public WaitingRoomScreen(RummikubGame game, String gameId, boolean isHost) {
        super(game);
        this.gameId = gameId;
        this.isHost = isHost;
    }

    // -------------------------------------------------------------------------
    // buildUI — Template Method hook
    // -------------------------------------------------------------------------

    @Override
    protected void buildUI() {
        // ---- Labels ----
        Label titleLabel  = makeLabel("RUANG TUNGGU");
        titleLabel.setFontScale(1.8f);

        Label gameIdLabel = makeLabel("Game ID: " + gameId);
        gameIdLabel.setColor(new Color(0.8f, 1f, 0.8f, 1f));

        playerListLabel = makeLabel("Memuat daftar pemain...");
        statusLabel     = makeLabel("Menunggu minimal 2 pemain...");
        statusLabel.setColor(Color.YELLOW);

        // ---- Buttons ----
        startButton = makeButton("MULAI GAME", new Color(0.15f, 0.55f, 0.15f, 1f));
        startButton.setVisible(isHost);
        startButton.setDisabled(true);

        TextButton backButton = makeButton("KEMBALI", new Color(0.40f, 0.40f, 0.40f, 1f));

        // ---- Layout ----
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(titleLabel).padBottom(20).row();
        table.add(gameIdLabel).padBottom(24).row();
        table.add(makeLabel("Pemain yang bergabung:")).padBottom(8).row();
        table.add(playerListLabel).padBottom(16).row();
        table.add(statusLabel).padBottom(24).row();

        if (isHost) {
            table.add(startButton).width(240).height(50).padBottom(12).row();
        }
        table.add(backButton).width(160).height(44).row();

        stage.addActor(table);

        // ---- Listeners ----
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onStartClicked();
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new LobbyScreen(game));
            }
        });

        // Trigger an immediate poll so the list populates right away
        pollWaitingRoom();
    }

    // -------------------------------------------------------------------------
    // update — Template Method hook (called every frame)
    // -------------------------------------------------------------------------

    @Override
    protected void update(float delta) {
        pollTimer += delta;
        if (pollTimer >= 3f) {
            pollTimer = 0f;
            pollWaitingRoom();
        }
    }

    // -------------------------------------------------------------------------
    // Polling
    // -------------------------------------------------------------------------

    private void pollWaitingRoom() {
        facade.getGameState(gameId, new ApiCallback<GameStateResponse>() {
            @Override
            public void onSuccess(GameStateResponse r) {
                if (r == null || !r.success || r.data == null) return;

                // Game already started (e.g., host started from another client)
                if ("IN_PROGRESS".equals(r.data.status)) {
                    GameStateManager.getInstance().loadFromServer(r.data);
                    game.setScreen(new GameScreen(game, gameId));
                    return;
                }

                // Update player list
                playerNames.clear();
                if (r.data.participants != null) {
                    for (ParticipantDto p : r.data.participants) {
                        playerNames.add(p.username + " (urutan " + p.turnOrder + ")");
                    }
                }
                updatePlayerListDisplay();

                // Enable start button for host when >= 2 players
                if (isHost) {
                    startButton.setDisabled(playerNames.size() < 2);
                    statusLabel.setText(playerNames.size() >= 2
                            ? "Siap dimulai! (" + playerNames.size() + " pemain)"
                            : "Menunggu minimal 2 pemain... (" + playerNames.size() + "/2)");
                } else {
                    statusLabel.setText("Menunggu host memulai game... (" + playerNames.size() + " pemain)");
                }
            }

            @Override
            public void onFailure(String err) {
                Gdx.app.log("WaitingRoom", "Poll error: " + err);
            }
        });
    }

    private void updatePlayerListDisplay() {
        if (playerNames.isEmpty()) {
            playerListLabel.setText("(belum ada pemain)");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < playerNames.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append("  ").append(i + 1).append(". ").append(playerNames.get(i));
        }
        playerListLabel.setText(sb.toString());
    }

    // -------------------------------------------------------------------------
    // Button handlers
    // -------------------------------------------------------------------------

    private void onStartClicked() {
        startButton.setDisabled(true);
        facade.startGame(gameId, new ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse r) {
                if (r.success) {
                    // Fetch fresh state then enter GameScreen
                    facade.getGameState(gameId, new ApiCallback<GameStateResponse>() {
                        @Override
                        public void onSuccess(GameStateResponse gs) {
                            if (gs != null && gs.success && gs.data != null) {
                                GameStateManager.getInstance().loadFromServer(gs.data);
                            }
                            game.setScreen(new GameScreen(game, gameId));
                        }

                        @Override
                        public void onFailure(String err) {
                            game.setScreen(new GameScreen(game, gameId));
                        }
                    });
                } else {
                    statusLabel.setText("Gagal memulai: " + (r.error != null ? r.error : "error"));
                    startButton.setDisabled(false);
                }
            }

            @Override
            public void onFailure(String err) {
                statusLabel.setText("Gagal memulai: " + err);
                startButton.setDisabled(false);
            }
        });
    }
}
