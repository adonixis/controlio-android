package ru.adonixis.controlio.model;

import java.util.List;

public class AddClientsRequest {
    private final String projectid;
    private final List<String> clients;

    public AddClientsRequest(String projectid, List<String> clients) {
        this.projectid = projectid;
        this.clients = clients;
    }
}