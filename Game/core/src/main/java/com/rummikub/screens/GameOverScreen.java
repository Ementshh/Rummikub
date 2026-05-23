package com.rummikub.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.rummikub.RummikubGame;
import com.rummikub.network.NetworkManager;
import com.rummikub.network.dto.ParticipantDto;
import com.rummikub.state.GameStateManager;
import com.rummikub.utils.ResourcePool;

/**
 * Game-over screen — shown when the game ends.
 *
 * Layout (Scene2D Table, centered):
 *   "RUMMIKUB!" or "GAME SELESAI" title
 *   Winner announcement
 *   Participant list
 *   "MAIN LAGI" button → LobbyScreen
 *   "KELUAR" button → exit
 */
public class GameOverScreen extends BaseScreen {

    private final String winnerUsername;

    public GameOverScreen(RummikubGame game, String winnerUsername) {
        super(game);
        this.winnerUsername = winnerUsername;
    }

    // -------------------------------------------------------------------------
    // buildUI — Template Method hook
    // -------------------------------------------------------------------------

    @Override
    protected void buildUI() {
        com.badlogic.gdx.scenes.scene2d.ui.Image bg = new com.badlogic.gdx.scenes.scene2d.ui.Image(com.rummikub.utils.ResourcePool.getInstance().getBgGameOver());
        bg.setFillParent(true);
        stage.addActor(bg);

        String myUsername = NetworkManager.getInstance().getCurrentUsername();
        boolean iWon = winnerUsername != null && winnerUsername.equals(myUsername);
        boolean isWinner = iWon;

        if (isWinner) {
            ResourcePool.getInstance().getSfxWin().play();
        } else {
            ResourcePool.getInstance().getSfxLose().play();
        }

        // ---- Title ----
        Label titleLabel = makeLabel(iWon ? "RUMMIKUB!" : "GAME SELESAI");
        titleLabel.setFontScale(2.5f);
        titleLabel.setColor(Color.WHITE);

        // ---- Winner label ----
        Label winnerLabel;
        if (winnerUsername != null && !winnerUsername.isEmpty()) {
            winnerLabel = makeLabel(iWon ? "Selamat, kamu menang!" : "Pemenang: " + winnerUsername);
        } else {
            winnerLabel = makeLabel("Tidak ada pemenang.");
        }
        winnerLabel.setColor(Color.WHITE);
        winnerLabel.setFontScale(1.3f);

        // ---- Participant list ----
        Label participantsTitle = makeLabel("Peserta:");
        StringBuilder sb = new StringBuilder();
        for (ParticipantDto p : GameStateManager.getInstance().getParticipants()) {
            sb.append("  ").append(p.username);
            if (p.username.equals(winnerUsername)) sb.append(" [MENANG]");
            sb.append("\n");
        }
        Label participantList = makeLabel(sb.length() > 0 ? sb.toString() : "(tidak ada data)");
        participantList.setColor(Color.WHITE);

        // ---- Buttons ----
        com.badlogic.gdx.scenes.scene2d.ui.Button playAgainButton = makeImageButton("btn_mainlagi");
        com.badlogic.gdx.scenes.scene2d.ui.Button exitButton = makeImageButton("btn_keluar");
        
        // ---- Layout ----
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(titleLabel).padBottom(20).row();
        table.add(winnerLabel).padBottom(30).row();
        table.add(participantsTitle).left().padBottom(8).row();
        table.add(participantList).left().padBottom(30).row();
        table.add(playAgainButton).padBottom(12).row();
        table.add(exitButton).row();

        stage.addActor(table);

        // ---- Listeners ----
        playAgainButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new LobbyScreen(game));
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                com.badlogic.gdx.Gdx.app.exit();
            }
        });
    }

    @Override
    protected void onShow() {
        ResourcePool.getInstance().stopBgm();
    }
}
