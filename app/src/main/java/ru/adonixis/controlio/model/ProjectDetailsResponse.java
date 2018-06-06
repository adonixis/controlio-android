package ru.adonixis.controlio.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProjectDetailsResponse implements Serializable {

    @SerializedName("_id")
    private String id;
    @SerializedName("updatedAt")
    private String updatedAt;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("title")
    private String title;
    @SerializedName("image")
    private String image;
    @SerializedName("description")
    private String description;
    @SerializedName("owner")
    private UserResponse owner;
    @SerializedName("__v")
    private Integer v;
    @SerializedName("lastPost")
    private PostResponse lastPost;
    @SerializedName("lastStatus")
    private PostResponse lastStatus;
    @SerializedName("isFinished")
    private Boolean finished;
    @SerializedName("canEdit")
    private Boolean canEdit;
    @SerializedName("posts")
    private List<String> posts = new ArrayList<>();
    @SerializedName("invites")
    private List<InviteResponse> invites = new ArrayList<>();
    @SerializedName("clients")
    private List<UserResponse> clients = new ArrayList<>();
    @SerializedName("managers")
    private List<UserResponse> managers = new ArrayList<>();
    @SerializedName("progress")
    private Integer progress;
    @SerializedName("progressEnabled")
    private Boolean progressEnabled;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UserResponse getOwner() {
        return owner;
    }

    public void setOwner(UserResponse owner) {
        this.owner = owner;
    }

    public Integer getV() {
        return v;
    }

    public void setV(Integer v) {
        this.v = v;
    }

    public PostResponse getLastPost() {
        return lastPost;
    }

    public void setLastPost(PostResponse lastPost) {
        this.lastPost = lastPost;
    }

    public PostResponse getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(PostResponse lastStatus) {
        this.lastStatus = lastStatus;
    }

    public Boolean isFinished() {
        return finished;
    }

    public void setFinished(Boolean finished) {
        this.finished = finished;
    }

    public Boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(Boolean canEdit) {
        this.canEdit = canEdit;
    }

    public List<String> getPosts() {
        return posts;
    }

    public void setPosts(List<String> posts) {
        this.posts = posts;
    }

    public List<InviteResponse> getInvites() {
        return invites;
    }

    public void setInvites(List<InviteResponse> invites) {
        this.invites = invites;
    }

    public List<UserResponse> getClients() {
        return clients;
    }

    public void setClients(List<UserResponse> clients) {
        this.clients = clients;
    }

    public List<UserResponse> getManagers() {
        return managers;
    }

    public void setManagers(List<UserResponse> managers) {
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
        return "ProjectDetailsResponse{" +
                "id='" + id + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", title='" + title + '\'' +
                ", image='" + image + '\'' +
                ", description='" + description + '\'' +
                ", owner=" + owner +
                ", v=" + v +
                ", lastPost=" + lastPost +
                ", lastStatus=" + lastStatus +
                ", archived=" + finished +
                ", canEdit=" + canEdit +
                ", posts=" + posts +
                ", invites=" + invites +
                ", clients=" + clients +
                ", managers=" + managers +
                ", progress=" + progress +
                ", progressEnabled=" + progressEnabled +
                '}';
    }
}