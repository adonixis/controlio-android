package ru.adonixis.controlio.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import ru.adonixis.controlio.model.NewPostRequest.PostType;

public class PostDetailsResponse implements Serializable {
    @SerializedName("_id")
    private String id;
    @SerializedName("updatedAt")
    private String updatedAt;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("author")
    private UserResponse author;
    @SerializedName("text")
    private String text;
    @SerializedName("__v")
    private Integer v;
    @SerializedName("attachments")
    private List<String> attachments;
    @SerializedName("isEdited")
    private Boolean edited;
    @SerializedName("type")
    @PostType
    private String type;

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

    public UserResponse getAuthor() {
        return author;
    }

    public void setAuthor(UserResponse author) {
        this.author = author;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getV() {
        return v;
    }

    public void setV(Integer v) {
        this.v = v;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    public Boolean isEdited() {
        return edited;
    }

    public void setEdited(Boolean isEdited) {
        this.edited = edited;
    }

    @PostType
    public String getType() {
        return type;
    }

    public void setType(@PostType String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "PostDetailsResponse{" +
                "id='" + id + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", author=" + author +
                ", text='" + text + '\'' +
                ", v=" + v +
                ", attachments=" + attachments +
                ", isEdited=" + edited +
                ", type='" + type + '\'' +
                '}';
    }
}