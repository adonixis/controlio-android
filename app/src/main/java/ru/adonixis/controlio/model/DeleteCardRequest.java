package ru.adonixis.controlio.model;

public class DeleteCardRequest {
    private final String customerid;
    private final String cardid;

    public DeleteCardRequest(String customerid, String cardid) {
        this.customerid = customerid;
        this.cardid = cardid;
    }
}