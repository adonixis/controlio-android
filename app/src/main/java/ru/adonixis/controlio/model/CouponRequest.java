package ru.adonixis.controlio.model;

public class CouponRequest {
    private final String userid;
    private final String coupon;

    public CouponRequest(String userid, String coupon) {
        this.userid = userid;
        this.coupon = coupon;
    }
}