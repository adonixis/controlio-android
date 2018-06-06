package ru.adonixis.controlio.model;

import java.util.List;

public class EditPostRequest {
    private final String projectid;
    private final String postid;
    private final String text;
    private final List<String> attachments;

    public EditPostRequest(String projectid, String postid, String text, List<String> attachments) {
        this.projectid = projectid;
        this.postid = postid;
        this.text = text;
        this.attachments = attachments;
    }
}