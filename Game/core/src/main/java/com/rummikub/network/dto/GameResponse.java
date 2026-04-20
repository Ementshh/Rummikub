package com.rummikub.network.dto;

public class GameResponse {
    public boolean success;
    public GameData data;
    public String error;

    public GameResponse() {}

    public static class GameData {
        public String id;   // game UUID

        public GameData() {}
    }
}
