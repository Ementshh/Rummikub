package com.rummikub.network.dto;

public class EndTurnResponse {
    public boolean success;
    public EndTurnData data;
    public String error;

    public EndTurnResponse() {}

    public static class EndTurnData {
        public String nextTurnUserId;
        public String winner;
        public boolean gameOver;

        public EndTurnData() {}
    }
}
