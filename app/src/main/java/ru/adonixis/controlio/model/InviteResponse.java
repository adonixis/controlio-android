package ru.adonixis.controlio.model;

import android.support.annotation.StringDef;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class InviteResponse implements Serializable {
    @StringDef({TYPE_MANAGE, TYPE_CLIENT, TYPE_OWNER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface InviteType {}
    public static final String TYPE_OWNER = "own";
    public static final String TYPE_MANAGE = "manage";
    public static final String TYPE_CLIENT = "client";

    @SerializedName("_id")
    private String id;
    @SerializedName("updatedAt")
    private String updatedAt;
    @SerializedName("createdAt")
    private String createdAt;
    @InviteType
    @SerializedName("type")
    private String type;
    @SerializedName("sender")
    private String sender;
    @SerializedName("project")
    private String project;
    @SerializedName("invitee")
    private UserResponse invitee;
    @SerializedName("__v")
    private Integer v;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @InviteType
    public String getType() {
        return type;
    }

    public void setType(@InviteType String type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public UserResponse getInvitee() {
        return invitee;
    }

    public void setInvitee(UserResponse invitee) {
        this.invitee = invitee;
    }

    public Integer getV() {
        return v;
    }

    public void setV(Integer v) {
        this.v = v;
    }

    @Override
    public String toString() {
        return "InviteResponse{" +
                "id='" + id + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", type='" + type + '\'' +
                ", sender='" + sender + '\'' +
                ", project='" + project + '\'' +
                ", invitee=" + invitee +
                ", v=" + v +
                '}';
    }
}