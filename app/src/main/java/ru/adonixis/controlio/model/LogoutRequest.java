package ru.adonixis.controlio.model;

public class LogoutRequest {
    private final String androidPushToken;

    public LogoutRequest(String androidPushToken) {
        this.androidPushToken = androidPushToken;
    }
}