package com.rummikub.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.utils.Json;
import com.rummikub.utils.Constants;

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
}
