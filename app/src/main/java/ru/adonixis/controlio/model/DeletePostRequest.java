package ru.adonixis.controlio.model;

public class DeletePostRequest {
    private final String projectid;
    private final String postid;

    public DeletePostRequest(String projectid, String postid) {
        this.projectid = projectid;
        this.postid = postid;
    }
}