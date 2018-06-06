package ru.adonixis.controlio.model;

public class StripeSourceRequest {
    private final String customerid;
    private final String source;

    public StripeSourceRequest(String customerid, String source) {
        this.customerid = customerid;
        this.source = source;
    }
}