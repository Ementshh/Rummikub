package com.rummikub.network;

import com.rummikub.network.dto.*;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

/**
 * [FACADE] — Hides all HTTP/JSON complexity from Screen classes.
 *
 * Not a singleton — screens can instantiate this directly.
 * All methods delegate to NetworkManager and handle token storage
 * transparently on successful login.
 */
public class GameApiFacade {

    private final NetworkManager net = NetworkManager.getInstance();

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    /**
     * Login with username/password.
     * On success, automatically stores the JWT token and userId in NetworkManager.
     */
    public void login(String username, String password, ApiCallback<LoginResponse> cb) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        net.post("/api/auth/login", body, LoginResponse.class, new ApiCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse r) {
                if (r.success && r.data != null) {
                    net.setToken(r.data.token);
                    net.setUserId(r.data.userId);
                    net.setCurrentUsername(username);
                }
                cb.onSuccess(r);
            }

            @Override
            public void onFailure(String err) {
                cb.onFailure(err);
            }
        });
    }

    /**
     * Register a new account.
     */
    public void register(String username, String password, ApiCallback<GenericResponse> cb) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        net.post("/api/auth/register", body, GenericResponse.class, cb);
    }

    // -------------------------------------------------------------------------
    // Game session
    // -------------------------------------------------------------------------

    /**
     * Create a new game room. Returns a GameResponse containing the game UUID.
     */
    public void createGame(ApiCallback<GameResponse> cb) {
        net.post("/api/games", null, GameResponse.class, cb);
    }

    /**
     * Join an existing game room by its UUID.
     */
    public void joinGame(String gameId, ApiCallback<GenericResponse> cb) {
        net.post("/api/games/" + gameId + "/join", null, GenericResponse.class, cb);
    }

    /**
     * Start the game (host only). Requires at least 2 participants.
     */
    public void startGame(String gameId, ApiCallback<GenericResponse> cb) {
        net.post("/api/games/" + gameId + "/start", null, GenericResponse.class, cb);
    }

    /**
     * Draw a tile from the pool. Automatically ends the current player's turn.
     */
    public void drawTile(String gameId, ApiCallback<GenericResponse> cb) {
        net.post("/api/games/" + gameId + "/draw", null, GenericResponse.class, cb);
    }

    /**
     * Submit the end-turn payload to the server.
     * The request must contain the full table state and remaining rack tiles.
     */
    public void endTurn(String gameId, EndTurnRequest req, ApiCallback<EndTurnResponse> cb) {
        // Manually build JSON to guarantee quoted keys (LibGDX Json emits unquoted keys
        // which Spring/Jackson rejects with 400). Backend expects snake_case field names.
        String json = endTurnToJson(req);
        com.badlogic.gdx.Gdx.app.log("GameApiFacade", "END-TURN JSON payload: " + json);
        net.postRaw("/api/games/" + gameId + "/end-turn", json, EndTurnResponse.class, cb);
    }

    /** Manually serializes EndTurnRequest to a spec-compliant JSON string. */
    private String endTurnToJson(EndTurnRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"table_sets\":[");
        if (req.table_sets != null) {
            for (int i = 0; i < req.table_sets.size(); i++) {
                if (i > 0) sb.append(",");
                var set = req.table_sets.get(i);
                sb.append("{\"set_type\":\"").append(set.set_type).append("\",");
                sb.append("\"tile_ids\":[");
                if (set.tile_ids != null) {
                    for (int j = 0; j < set.tile_ids.size(); j++) {
                        if (j > 0) sb.append(",");
                        sb.append(set.tile_ids.get(j));
                    }
                }
                sb.append("]}");
            }
        }
        sb.append("],\"rack_tiles\":[");
        if (req.rack_tiles != null) {
            for (int i = 0; i < req.rack_tiles.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(req.rack_tiles.get(i));
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Polling
    // -------------------------------------------------------------------------

    /**
     * Poll the current game state. Called periodically during gameplay.
     */
    public void getGameState(String gameId, ApiCallback<GameStateResponse> cb) {
        net.get("/api/games/" + gameId + "/state", GameStateResponse.class, cb);
    }
}
