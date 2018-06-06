package ru.adonixis.controlio.model;

public class DeleteManagerRequest {
    private final String projectid;
    private final String managerid;

    public DeleteManagerRequest(String projectid, String managerid) {
        this.projectid = projectid;
        this.managerid = managerid;
    }
}