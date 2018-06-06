package ru.adonixis.controlio.model;

public class DeleteClientRequest {
    private final String projectid;
    private final String clientid;

    public DeleteClientRequest(String projectid, String clientid) {
        this.projectid = projectid;
        this.clientid = clientid;
    }
}