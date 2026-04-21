package com.rummikub.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.rummikub.RummikubGame;
import com.rummikub.network.NetworkManager;
import com.rummikub.network.dto.ParticipantDto;
import com.rummikub.state.GameStateManager;

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
        String myUsername = NetworkManager.getInstance().getCurrentUsername();
        boolean iWon = winnerUsername != null && winnerUsername.equals(myUsername);

        // ---- Title ----
        Label titleLabel = makeLabel(iWon ? "RUMMIKUB!" : "GAME SELESAI");
        titleLabel.setFontScale(2.5f);
        titleLabel.setColor(iWon ? Color.YELLOW : Color.WHITE);

        // ---- Winner label ----
        Label winnerLabel;
        if (winnerUsername != null && !winnerUsername.isEmpty()) {
            winnerLabel = makeLabel(iWon ? "Selamat, kamu menang!" : "Pemenang: " + winnerUsername);
        } else {
            winnerLabel = makeLabel("Tidak ada pemenang.");
        }
        winnerLabel.setColor(iWon ? Color.GREEN : Color.LIGHT_GRAY);
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
        participantList.setColor(Color.LIGHT_GRAY);

        // ---- Buttons ----
        TextButton playAgainButton = makeButton("MAIN LAGI", new Color(0.15f, 0.50f, 0.15f, 1f));
        TextButton exitButton      = makeButton("KELUAR",    new Color(0.50f, 0.15f, 0.15f, 1f));

        // ---- Layout ----
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        table.add(titleLabel).padBottom(20).row();
        table.add(winnerLabel).padBottom(30).row();
        table.add(participantsTitle).left().padBottom(8).row();
        table.add(participantList).left().padBottom(30).row();
        table.add(playAgainButton).width(200).height(50).padBottom(12).row();
        table.add(exitButton).width(200).height(50).row();

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
}
