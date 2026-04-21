package com.rummikub.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.rummikub.utils.Constants;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * [SINGLETON] — Single HTTP client instance for the entire application lifecycle.
 *
 * All network calls run on a background thread to avoid blocking the LibGDX
 * render thread. Results are posted back to the main thread via
 * Gdx.app.postRunnable() before invoking the callback.
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
        if (body instanceof java.util.Map) {
            // Build a proper JSON object manually — guaranteed quoted keys
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) body;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
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
     * Performs an HTTP POST on a background thread.
     * Body is serialized to JSON via LibGDX Json. Pass {@code null} for an empty body.
     */
    public <T> void post(String endpoint, Object body, Class<T> responseType, ApiCallback<T> cb) {
        new Thread(() -> {
            try {
                URL url = new URL(Constants.BASE_URL + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                if (jwtToken != null) {
                    conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                }
                conn.setDoOutput(true);
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);

                String bodyJson = (body != null) ? toStandardJson(body) : "{}";
                conn.getOutputStream().write(bodyJson.getBytes(StandardCharsets.UTF_8));

                int status = conn.getResponseCode();
                InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
                String resp = (is != null)
                        ? new String(is.readAllBytes(), StandardCharsets.UTF_8)
                        : "";

                Gdx.app.log("NetworkManager", "POST " + endpoint + " -> HTTP " + status + " | body: " + resp);

                // If the server returned a 2xx with no body, synthesise {"success":true}
                if (resp.isBlank()) {
                    resp = (status >= 200 && status < 300) ? "{\"success\":true}" : "{\"success\":false}";
                }

                T result = json.fromJson(responseType, resp);
                Gdx.app.postRunnable(() -> cb.onSuccess(result));

            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Gdx.app.postRunnable(() -> cb.onFailure(msg));
            }
        }).start();
    }

    /**
     * Performs an HTTP GET on a background thread.
     */
    public <T> void get(String endpoint, Class<T> responseType, ApiCallback<T> cb) {
        new Thread(() -> {
            try {
                URL url = new URL(Constants.BASE_URL + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                if (jwtToken != null) {
                    conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                }
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);

                int status = conn.getResponseCode();
                InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
                String resp = (is != null)
                        ? new String(is.readAllBytes(), StandardCharsets.UTF_8)
                        : "{}";

                if (resp.isBlank() || resp.equals("{}")) {
                    resp = (status >= 200 && status < 300) ? "{\"success\":true}" : "{\"success\":false}";
                }

                T result = json.fromJson(responseType, resp);
                Gdx.app.postRunnable(() -> cb.onSuccess(result));

            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Gdx.app.postRunnable(() -> cb.onFailure(msg));
            }
        }).start();
    }
}
