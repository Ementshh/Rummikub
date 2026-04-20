package com.rummikub.network;

/**
 * Generic callback interface for async network operations.
 * onSuccess is always called on the LibGDX main thread via Gdx.app.postRunnable().
 * onFailure is also called on the main thread.
 */
public interface ApiCallback<T> {
    void onSuccess(T result);
    void onFailure(String errorMessage);
}
