package com.rummikub.screens.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.rummikub.network.dto.TileDto;
import com.rummikub.state.GameStateManager;

import java.util.ArrayList;
import java.util.List;
import com.rummikub.network.GameApiFacade;
import com.rummikub.network.ApiCallback;
import com.rummikub.network.dto.GenericResponse;


public class CheatCodeHandler extends InputAdapter {

    
    public interface CheatActivatedListener {
        void onCheatActivated();
    }

    private static final int[] CHEAT_SEQUENCE = {
        Input.Keys.J, Input.Keys.O, Input.Keys.K, Input.Keys.E, Input.Keys.R
    };

    private static final float SEQUENCE_TIMEOUT = 3f; // seconds to complete the sequence

    private final GameStateManager gsm;
    private final CheatActivatedListener listener;
    private final GameApiFacade facade;
    private final String gameId;

    private int currentIndex = 0;
    private float timeSinceLastKey = 0f;

    public CheatCodeHandler(GameStateManager gsm, GameApiFacade facade, String gameId, CheatActivatedListener listener) {
        this.gsm = gsm;
        this.facade = facade;
        this.gameId = gameId;
        this.listener = listener;
    }

   
    public void update(float delta) {
        if (currentIndex > 0) {
            timeSinceLastKey += delta;
            if (timeSinceLastKey > SEQUENCE_TIMEOUT) {
                currentIndex = 0; // Reset sequence on timeout
            }
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        Gdx.app.log("CheatCodeHandler", "Key pressed: " + Input.Keys.toString(keycode) + " (Code: " + keycode + ")");
        
        if (keycode == CHEAT_SEQUENCE[currentIndex]) {
            currentIndex++;
            timeSinceLastKey = 0f;

            if (currentIndex >= CHEAT_SEQUENCE.length) {
                activateCheat();
                currentIndex = 0;
                return true;
            }
        } else {
           
            currentIndex = 0;
           
            if (keycode == CHEAT_SEQUENCE[0]) {
                currentIndex = 1;
                timeSinceLastKey = 0f;
            }
        }
        return false;
    }

   
    private void activateCheat() {
        Gdx.app.log("CheatCodeHandler", "CHEAT ACTIVATED — calling backend...");

        facade.cheat(gameId, new ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse response) {
                if (response.success) {
                    Gdx.app.log("CheatCodeHandler", "Backend cheat successful, reloading state.");
                    listener.onCheatActivated();
                } else {
                    Gdx.app.log("CheatCodeHandler", "Backend cheat failed: " + response.error);
                }
            }

            @Override
            public void onFailure(String error) {
                Gdx.app.log("CheatCodeHandler", "Network error during cheat: " + error);
            }
        });
    }
}
