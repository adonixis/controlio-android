package ru.adonixis.controlio.model;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SubscriptionRequest {
    private final String userid;
    @PlanId private final int planid;

    @IntDef({PLAN_FREE, PLAN_TWENTY, PLAN_FIFTY, PLAN_HUNDRED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlanId {}
    public static final int PLAN_FREE = 0;
    public static final int PLAN_TWENTY = 1;
    public static final int PLAN_FIFTY = 2;
    public static final int PLAN_HUNDRED = 3;

    public SubscriptionRequest(String userid, @PlanId int planid) {
        this.userid = userid;
        this.planid = planid;
    }
}