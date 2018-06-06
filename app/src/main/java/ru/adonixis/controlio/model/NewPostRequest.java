package ru.adonixis.controlio.model;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public class NewPostRequest {
    private final String projectid;
    private final String text;
    private final List<String> attachments;
    @PostType private final String type;

    @StringDef({TYPE_POST, TYPE_STATUS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PostType {}
    public static final String TYPE_POST = "post";
    public static final String TYPE_STATUS = "status";

    public NewPostRequest(String projectid, String text, List<String> attachments, @PostType String type) {
        this.projectid = projectid;
        this.text = text;
        this.attachments = attachments;
        this.type = type;
    }
}