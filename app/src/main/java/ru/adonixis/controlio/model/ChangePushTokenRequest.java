package ru.adonixis.controlio.model;

public class ChangePushTokenRequest {
    private final String androidPushToken;
    private final String oldAndroidPushToken;

    public ChangePushTokenRequest(String androidPushToken, String oldAndroidPushToken) {
        this.androidPushToken = androidPushToken;
        this.oldAndroidPushToken = oldAndroidPushToken;
    }
}