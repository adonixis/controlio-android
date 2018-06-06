package ru.adonixis.controlio.model;

public class LoginRequest {
    private final String email;
    private final String password;
    private final String androidPushToken;

    public LoginRequest(String email, String password, String androidPushToken) {
        this.email = email;
        this.password = password;
        this.androidPushToken = androidPushToken;
    }
}