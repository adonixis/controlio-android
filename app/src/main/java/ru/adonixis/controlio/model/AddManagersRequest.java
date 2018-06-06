package ru.adonixis.controlio.model;

import java.util.List;

public class AddManagersRequest {
    private final String projectid;
    private final List<String> managers;

    public AddManagersRequest(String projectid, List<String> managers) {
        this.projectid = projectid;
        this.managers = managers;
    }
}