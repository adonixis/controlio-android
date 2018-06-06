package ru.adonixis.controlio.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ProjectResponse implements Serializable {

    @SerializedName("canEdit")
    private Boolean canEdit;
    @SerializedName("_id")
    private String id;
    @SerializedName("updatedAt")
    private String updatedAt;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("description")
    private String description;
    @SerializedName("title")
    private String title;
    @SerializedName("image")
    private String image;
    @SerializedName("owner")
    private String owner;
    @SerializedName("lastPost")
    private PostDetailsResponse lastPost;
    @SerializedName("lastStatus")
    private PostDetailsResponse lastStatus;
    @SerializedName("isFinished")
    private Boolean finished;
    @SerializedName("managers")
    private List<String> managers;
    @SerializedName("progress")
    private Integer progress;
    @SerializedName("progressEnabled")
    private Boolean progressEnabled;

    public Boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public PostDetailsResponse getLastPost() {
        return lastPost;
    }

    public void setLastPost(PostDetailsResponse lastPost) {
        this.lastPost = lastPost;
    }

    public PostDetailsResponse getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(PostDetailsResponse lastStatus) {
        this.lastStatus = lastStatus;
    }

    public Boolean isFinished() {
        return finished;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }

    public List<String> getManagers() {
        return managers;
    }

    public void setManagers(List<String> managers) {
        this.managers = managers;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Boolean isProgressEnabled() {
        return progressEnabled;
    }

    public void setProgressEnabled(Boolean progressEnabled) {
        this.progressEnabled = progressEnabled;
    }

    @Override
    public String toString() {
        return "ProjectResponse{" +
                "canEdit=" + canEdit +
                ", id='" + id + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", description='" + description + '\'' +
                ", title='" + title + '\'' +
                ", image='" + image + '\'' +
                ", owner='" + owner + '\'' +
                ", lastPost=" + lastPost +
                ", lastStatus=" + lastStatus +
                ", archived=" + finished +
                ", managers=" + managers +
                ", progress=" + progress +
                ", progressEnabled=" + progressEnabled +
                '}';
    }
}