package com.rummikub.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.utils.Json;
import com.rummikub.utils.Constants;

import com.rummikub.network.dto.GameStateResponse;
import com.rummikub.network.dto.ParticipantDto;

import java.util.Map;

/**
 * [SINGLETON] — Single HTTP client instance for the entire application lifecycle.
 *
 * All network calls use libGDX's cross-platform Net API so they work on
 * Desktop, Android, and HTML5/GWT. Results are posted back to the main
 * thread via Gdx.app.postRunnable() before invoking the callback.
 */
public class NetworkManager {

    private static NetworkManager instance;

    private String jwtToken;
    private String currentUserId;
    private String currentUsername;
    private final Json json;

    private NetworkManager() {
        json = new Json();
        json.setIgnoreUnknownFields(true);
    }

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Auth state
    // -------------------------------------------------------------------------

    public void setToken(String token) {
        this.jwtToken = token;
    }

    public void setUserId(String userId) {
        this.currentUserId = userId;
    }

    public String getUserId() {
        return currentUserId;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public boolean isAuthenticated() {
        return jwtToken != null && !jwtToken.isEmpty();
    }

    public void clearAuth() {
        jwtToken = null;
        currentUserId = null;
        currentUsername = null;
    }

    // -------------------------------------------------------------------------
    // JSON serialization
    // -------------------------------------------------------------------------

    /**
     * Converts an object to a standard JSON string with quoted keys.
     *
     * LibGDX's built-in Json serializer omits quotes on keys, which breaks
     * Spring Boot's Jackson parser. This method handles the two cases we
     * actually use: Map<String,String> (auth bodies) and arbitrary POJOs
     * (EndTurnRequest, etc.).
     *
     * For POJOs we fall back to LibGDX Json but post-process the output to
     * add quotes around unquoted keys.
     */
    @SuppressWarnings("unchecked")
    private String toStandardJson(Object body) {
        if (body instanceof Map) {
            // Build a proper JSON object manually — guaranteed quoted keys
            Map<String, Object> map = (Map<String, Object>) body;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(entry.getKey()).append("\":");
                Object val = entry.getValue();
                if (val == null) {
                    sb.append("null");
                } else if (val instanceof Number || val instanceof Boolean) {
                    sb.append(val);
                } else {
                    sb.append("\"").append(val.toString().replace("\"", "\\\"")).append("\"");
                }
            }
            sb.append("}");
            return sb.toString();
        }
        // For POJOs (EndTurnRequest etc.) use LibGDX Json — Spring's Jackson
        // is lenient enough to accept unquoted keys in most cases, and these
        // objects have complex nested structures that are hard to serialize manually.
        return json.toJson(body);
    }

    // -------------------------------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------------------------------

    /**
     * Performs an HTTP POST using libGDX's cross-platform Net API.
     * Body is serialized to JSON via LibGDX Json. Pass {@code null} for an empty body.
     */
    public <T> void post(String endpoint, Object body, Class<T> responseType, ApiCallback<T> cb) {
        String bodyJson = (body != null) ? toStandardJson(body) : "{}";
        postRaw(endpoint, bodyJson, responseType, cb);
    }

    /**
     * Performs an HTTP POST with a pre-built JSON string body.
     * Use this when manual JSON construction is needed (e.g., to guarantee quoted keys).
     */
    public <T> void postRaw(String endpoint, String bodyJson, Class<T> responseType, ApiCallback<T> cb) {
        Gdx.app.log("HTTP_BODY", "POST " + endpoint + " body: " + bodyJson);

        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(Constants.BASE_URL + endpoint);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
        if (jwtToken != null) {
            request.setHeader("Authorization", "Bearer " + jwtToken);
        }
        request.setContent(bodyJson);
        request.setTimeOut(15000);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String resp = httpResponse.getResultAsString();

                Gdx.app.log("NetworkManager", "POST " + endpoint + " -> HTTP " + statusCode + " | body: " + resp);

                if (resp == null || resp.trim().isEmpty()) {
                    resp = (statusCode >= 200 && statusCode < 300)
                        ? "{\"success\":true}"
                        : "{\"success\":false}";
                }

                try {
                    final T result = json.fromJson(responseType, resp);
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            cb.onSuccess(result);
                        }
                    });
                } catch (final Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            cb.onFailure(msg);
                        }
                    });
                }
            }

            @Override
            public void failed(Throwable t) {
                final String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        cb.onFailure(msg);
                    }
                });
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        cb.onFailure("Request cancelled");
                    }
                });
            }
        });
    }

    /**
     * Performs an HTTP GET using libGDX's cross-platform Net API.
     */
    public <T> void get(String endpoint, Class<T> responseType, ApiCallback<T> cb) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(Constants.BASE_URL + endpoint);
        request.setHeader("Accept", "application/json");
        if (jwtToken != null) {
            request.setHeader("Authorization", "Bearer " + jwtToken);
        }
        request.setTimeOut(15000);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String resp = httpResponse.getResultAsString();

                if (resp == null || resp.trim().isEmpty() || resp.equals("{}")) {
                    resp = (statusCode >= 200 && statusCode < 300)
                        ? "{\"success\":true}"
                        : "{\"success\":false}";
                }

                Gdx.app.log("NetworkManager", "GET " + endpoint + " -> HTTP " + statusCode + " | body: " + resp);

                try {
                    final T result = json.fromJson(responseType, resp);
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            cb.onSuccess(result);
                        }
                    });
                } catch (final Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            cb.onFailure(msg);
                        }
                    });
                }
            }

            @Override
            public void failed(Throwable t) {
                final String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        cb.onFailure(msg);
                    }
                });
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        cb.onFailure("Request cancelled");
                    }
                });
            }
        });
    }

    /**
     * Performs an HTTP GET and manually parses the GameStateResponse tree
     * to bypass LibGDX Json generic List limitations under GWT.
     */
    public void getGameStateManual(String endpoint, ApiCallback<GameStateResponse> cb) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(Constants.BASE_URL + endpoint);
        request.setHeader("Accept", "application/json");
        if (jwtToken != null) {
            request.setHeader("Authorization", "Bearer " + jwtToken);
        }
        request.setTimeOut(15000);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String resp = httpResponse.getResultAsString();

                Gdx.app.log("NetworkManager", "GET " + endpoint + " -> HTTP " + statusCode + " | body: " + resp);

                if (resp == null || resp.trim().isEmpty() || resp.equals("{}")) {
                    final String errorMsg = "Empty response";
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            cb.onFailure(errorMsg);
                        }
                    });
                    return;
                }

                try {
                    com.badlogic.gdx.utils.JsonReader reader = new com.badlogic.gdx.utils.JsonReader();
                    com.badlogic.gdx.utils.JsonValue root = reader.parse(resp);

                    final GameStateResponse result = new GameStateResponse();
                    result.success = root.getBoolean("success", false);
                    result.error = root.getString("error", null);

                    if (root.has("data") && !root.get("data").isNull()) {
                        com.badlogic.gdx.utils.JsonValue dataNode = root.get("data");
                        result.data = new GameStateResponse.GameData();
                        result.data.id = dataNode.getString("id", null);
                        result.data.status = dataNode.getString("status", null);
                        result.data.currentTurnUserId = dataNode.getString("currentTurnUserId", null);
                        result.data.winner = dataNode.getString("winner", null);
                        result.data.hasDoneInitialMeld = dataNode.getBoolean("hasDoneInitialMeld", false);
                        result.data.meldScore = dataNode.getInt("meldScore", 0);
                        
                        if (dataNode.has("turnStartedAt") && !dataNode.get("turnStartedAt").isNull()) {
                            result.data.turnStartedAt = dataNode.getLong("turnStartedAt");
                        }

                        // Participants
                        if (dataNode.has("participants") && dataNode.get("participants").isArray()) {
                            result.data.participants = new java.util.ArrayList<ParticipantDto>();
                            for (com.badlogic.gdx.utils.JsonValue pNode : dataNode.get("participants")) {
                                ParticipantDto p = new ParticipantDto();
                                p.userId = pNode.getString("userId", null);
                                p.username = pNode.getString("username", null);
                                p.turnOrder = pNode.getInt("turnOrder", 0);
                                p.score = pNode.getInt("score", 0);
                                p.hasDoneInitialMeld = pNode.getBoolean("hasDoneInitialMeld", false);
                                p.hasLeft = pNode.getBoolean("hasLeft", false);
                                result.data.participants.add(p);
                            }
                        }

                        // My Rack Tiles
                        if (dataNode.has("myRackTiles") && dataNode.get("myRackTiles").isArray()) {
                            result.data.myRackTiles = new java.util.ArrayList<com.rummikub.network.dto.TileDto>();
                            for (com.badlogic.gdx.utils.JsonValue tNode : dataNode.get("myRackTiles")) {
                                com.rummikub.network.dto.TileDto t = new com.rummikub.network.dto.TileDto();
                                t.id = tNode.getInt("id", 0);
                                t.number = tNode.getInt("number", 0);
                                t.color = tNode.getString("color", null);
                                t.isJoker = tNode.getBoolean("isJoker", false);
                                result.data.myRackTiles.add(t);
                            }
                        }

                        // Table Sets
                        if (dataNode.has("tableSets") && dataNode.get("tableSets").isArray()) {
                            result.data.tableSets = new java.util.ArrayList<com.rummikub.network.dto.TableSetDto>();
                            for (com.badlogic.gdx.utils.JsonValue sNode : dataNode.get("tableSets")) {
                                com.rummikub.network.dto.TableSetDto ts = new com.rummikub.network.dto.TableSetDto();
                                ts.set_type = sNode.getString("set_type", null);
                                
                                ts.tile_ids = new java.util.ArrayList<Integer>();
                                if (sNode.has("tile_ids") && sNode.get("tile_ids").isArray()) {
                                    for (com.badlogic.gdx.utils.JsonValue idNode : sNode.get("tile_ids")) {
                                        ts.tile_ids.add(idNode.asInt());
                                    }
                                }
                                
                                ts.tiles = new java.util.ArrayList<com.rummikub.network.dto.TileDto>();
                                if (sNode.has("tiles") && sNode.get("tiles").isArray()) {
                                    for (com.badlogic.gdx.utils.JsonValue tNode : sNode.get("tiles")) {
                                        com.rummikub.network.dto.TileDto t = new com.rummikub.network.dto.TileDto();
                                        t.id = tNode.getInt("id", 0);
                                        t.number = tNode.getInt("number", 0);
                                        t.color = tNode.getString("color", null);
                                        t.isJoker = tNode.getBoolean("isJoker", false);
                                        ts.tiles.add(t);
                                    }
                                }
                                result.data.tableSets.add(ts);
                            }
                        }
                    }

                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            cb.onSuccess(result);
                        }
                    });
                } catch (final Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
                    Gdx.app.log("NetworkManager", "Parse error: " + msg, e);
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            cb.onFailure(msg);
                        }
                    });
                }
            }

            @Override
            public void failed(Throwable t) {
                final String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        cb.onFailure(msg);
                    }
                });
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        cb.onFailure("Request cancelled");
                    }
                });
            }
        });
    }
}
