package ru.adonixis.controlio.model;

public class EditProjectRequest {
    private final String projectid;
    private final String title;
    private final String description;
    private final String image;
    private final Boolean progressEnabled;

    public EditProjectRequest(String projectid, String title, String description, String image, Boolean progressEnabled) {
        this.projectid = projectid;
        this.title = title;
        this.description = description;
        this.image = image;
        this.progressEnabled = progressEnabled;
    }
}