package ru.adonixis.controlio.model;

public class LoginMagicLinkRequest {
    private final String token;
    private final String androidPushToken;

    public LoginMagicLinkRequest(String token, String androidPushToken) {
        this.token = token;
        this.androidPushToken = androidPushToken;
    }
}