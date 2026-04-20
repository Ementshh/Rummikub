package com.rummikub.network.dto;

public class LoginResponse {
    public boolean success;
    public LoginData data;
    public String error;

    public LoginResponse() {}

    public static class LoginData {
        public String token;
        public String userId;

        public LoginData() {}
    }
}
