package ru.adonixis.controlio.model;

import com.google.gson.annotations.SerializedName;

public class OkResponse {

    @SerializedName("success")
    private Boolean success;

    public Boolean isSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "OkResponse{" +
                "success=" + success +
                '}';
    }
}