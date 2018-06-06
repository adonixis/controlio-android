package ru.adonixis.controlio.model;

import com.google.gson.annotations.SerializedName;

public class FeaturesResponse {

    @SerializedName("0")
    private Boolean iosPaymentsAvailable;
    @SerializedName("1")
    private Boolean androidPaymentsAvailable;

    public Boolean isIosPaymentsAvailable() {
        return iosPaymentsAvailable;
    }

    public void setIosPaymentsAvailable(Boolean iosPaymentsAvailable) {
        this.iosPaymentsAvailable = iosPaymentsAvailable;
    }

    public Boolean isAndroidPaymentsAvailable() {
        return androidPaymentsAvailable;
    }

    public void setAndroidPaymenstAvailable(Boolean androidPaymentsAvailable) {
        this.androidPaymentsAvailable = androidPaymentsAvailable;
    }

    @Override
    public String toString() {
        return "FeaturesResponse{" +
                "iosPaymentsAvailable=" + iosPaymentsAvailable +
                ", androidPaymentsAvailable=" + androidPaymentsAvailable +
                '}';
    }
}