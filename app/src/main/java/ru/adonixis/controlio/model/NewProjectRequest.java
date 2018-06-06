package ru.adonixis.controlio.model;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class NewProjectRequest {
    private final String title;
    @UserType private final String type;
    private final String image;
    private final String status;
    private final String description;
    private final String managerEmail;
    private final List<String> clientEmails;
    private final Boolean progressEnabled;

    @StringDef({TYPE_MANAGER, TYPE_CLIENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface UserType {}
    public static final String TYPE_MANAGER = "manager";
    public static final String TYPE_CLIENT = "client";

    public NewProjectRequest(String title, @UserType String type, String image, String status, String description, String managerEmail, List<String> clientEmails, Boolean progressEnabled) {
        this.title = title;
        this.type = type;
        this.image = image;
        this.status = status;
        this.description = description;
        this.managerEmail = managerEmail;
        this.clientEmails = clientEmails;
        this.progressEnabled = progressEnabled;
    }
}