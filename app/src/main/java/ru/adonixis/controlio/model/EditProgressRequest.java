package ru.adonixis.controlio.model;

public class EditProgressRequest {
    private final String projectid;
    private final Integer progress;

    public EditProgressRequest(String projectid, Integer progress) {
        this.projectid = projectid;
        this.progress = progress;
    }
}