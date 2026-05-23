package com.rummikub.screens.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.rummikub.network.dto.TableSetDto;
import com.rummikub.network.dto.TileDto;
import com.rummikub.state.GameStateManager;

/**
 * Manages all HUD labels: timer, turn info, meld status, and opponent name.
 * Single Responsibility: UI text state only.
 */
public class GameHudManager {

    private final Label playerCountLabel;
    private final Label timerLabel;
    private final Label turnInfoLabel;
    private final Label statusLabel;
    private final Label playerNameLabel;
    private final GameStateManager gsm;
    private int totalPlayers = -1;

    public GameHudManager(Label playerCountLabel, Label timerLabel, Label turnInfoLabel,
                          Label statusLabel, Label playerNameLabel,
                          GameStateManager gsm) {
        this.playerCountLabel = playerCountLabel;
        this.timerLabel = timerLabel;
        this.turnInfoLabel = turnInfoLabel;
        this.statusLabel = statusLabel;
        this.playerNameLabel = playerNameLabel;
        this.gsm = gsm;
    }

    /** Updates the on-screen countdown timer label. */
    public void updateTimerDisplay(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        timerLabel.setText(String.format("TIMER: %02d:%02d", m, s));
        timerLabel.setColor(Color.WHITE);
    }

    /** Updates the turn info labels. */
    public void updateTurnInfo() {
        if (gsm.isMyTurn()) {
            turnInfoLabel.setText("GILIRAN: KAMU");
            turnInfoLabel.setColor(Color.WHITE);
        } else {
            String name = gsm.resolveCurrentTurnUsername();
            turnInfoLabel.setText("GILIRAN: " + name.toUpperCase());
            turnInfoLabel.setColor(Color.WHITE);
        }
    }

    /** Updates the meld points display, including carry-over from previous turns. */
    public void updateMeldPointsDisplay() {
        if (!gsm.isHasDoneInitialMeld()) {
            // Points from tiles placed THIS turn
            int newPoints = 0;
            int newTileCount = 0;
            for (TableSetDto set : gsm.getTableSets()) {
                if (set.isNewThisTurn) {
                    newTileCount += set.tile_ids.size();
                    for (int id : set.tile_ids) {
                        TileDto t = gsm.getTileById(id);
                        if (t != null && !t.isJoker) newPoints += t.number;
                    }
                }
            }

            // Total = accumulated score from previous turns + this turn's new points
            int totalMeld = gsm.getMeldScore() + newPoints;

            if (newTileCount == 0 && gsm.getMeldScore() == 0) {
                statusLabel.setText("Meld: BELUM — harus draw atau taruh tile");
                statusLabel.setColor(Color.WHITE);
            } else if (newTileCount == 0) {
                statusLabel.setText("Meld: " + gsm.getMeldScore() + "/30 poin (sebelumnya)");
                statusLabel.setColor(Color.WHITE);
            } else {
                statusLabel.setText("Meld: " + totalMeld + "/30 poin");
                statusLabel.setColor(Color.WHITE);
            }
        } else {
            statusLabel.setText("Meld: SUDAH \u2713");
            statusLabel.setColor(Color.WHITE);
        }
    }

    /** Displays a status message in the button bar. */
    public void showStatusMessage(String msg) {
        statusLabel.setText(msg);
        Gdx.app.log("GameScreen", "Status: " + msg);
    }

    /** Updates the player count label based on current participants. */
    public void updateParticipants() {
        if (totalPlayers == -1) {
            totalPlayers = gsm.getParticipants().size();
        }
        // Count active (non-left) participants
        int activeCount = 0;
        for (com.rummikub.network.dto.ParticipantDto p : gsm.getParticipants()) {
            if (!p.hasLeft) {
                activeCount++;
            }
        }
        playerCountLabel.setText("Players: " + activeCount + "/" + totalPlayers);
        playerCountLabel.setColor(Color.WHITE);
    }
}
