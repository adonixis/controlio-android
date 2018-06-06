package ru.adonixis.controlio.model;

import com.google.gson.annotations.SerializedName;

public class FacebookLoginRequest {
    @SerializedName("access_token")
    private final String accessToken;
    private final String androidPushToken;

    public FacebookLoginRequest(String accessToken, String androidPushToken) {
        this.accessToken = accessToken;
        this.androidPushToken = androidPushToken;
    }
}