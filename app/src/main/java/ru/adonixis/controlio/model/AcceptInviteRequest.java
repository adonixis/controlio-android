package ru.adonixis.controlio.model;

public class AcceptInviteRequest {
    private final String inviteid;
    private final boolean accept;

    public AcceptInviteRequest(String inviteid, boolean accept) {
        this.inviteid = inviteid;
        this.accept = accept;
    }
}