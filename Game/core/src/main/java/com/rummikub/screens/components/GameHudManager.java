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

    private final Label timerLabel;
    private final Label turnInfoLabel;
    private final Label statusLabel;
    private final Label opponentNameLabel;
    private final GameStateManager gsm;

    public GameHudManager(Label timerLabel, Label turnInfoLabel,
                          Label statusLabel, Label opponentNameLabel,
                          GameStateManager gsm) {
        this.timerLabel = timerLabel;
        this.turnInfoLabel = turnInfoLabel;
        this.statusLabel = statusLabel;
        this.opponentNameLabel = opponentNameLabel;
        this.gsm = gsm;
    }

    /** Updates the on-screen countdown timer label. */
    public void updateTimerDisplay(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        timerLabel.setText(String.format("TIMER: %02d:%02d", m, s));
        timerLabel.setColor(seconds <= 15 ? Color.RED : Color.YELLOW);
    }

    /** Updates the turn info and opponent name labels. */
    public void updateTurnInfo() {
        // Username resolution kasih ke gamestatemanager
        opponentNameLabel.setText(gsm.resolveOpponentUsername());

        if (gsm.isMyTurn()) {
            turnInfoLabel.setText("GILIRAN: KAMU");
            turnInfoLabel.setColor(Color.GREEN);
        } else {
            String name = gsm.resolveCurrentTurnUsername();
            turnInfoLabel.setText("GILIRAN: " + name.toUpperCase());
            turnInfoLabel.setColor(Color.LIGHT_GRAY);
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
                statusLabel.setColor(Color.YELLOW);
            } else if (newTileCount == 0) {
                statusLabel.setText("Meld: " + gsm.getMeldScore() + "/30 poin (sebelumnya)");
                statusLabel.setColor(Color.YELLOW);
            } else {
                statusLabel.setText("Meld: " + totalMeld + "/30 poin");
                statusLabel.setColor(totalMeld >= 30 ? Color.GREEN : Color.YELLOW);
            }
        } else {
            statusLabel.setText("Meld: SUDAH \u2713");
            statusLabel.setColor(Color.GREEN);
        }
    }

    /** Displays a status message in the button bar. */
    public void showStatusMessage(String msg) {
        statusLabel.setText(msg);
        Gdx.app.log("GameScreen", "Status: " + msg);
    }
}
