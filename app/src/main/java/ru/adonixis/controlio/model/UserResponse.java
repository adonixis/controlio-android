package ru.adonixis.controlio.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserResponse implements Serializable {

    @SerializedName("_id")
    private String id;
    @SerializedName("email")
    private String email;
    @SerializedName("stripeId")
    private String stripeId;
    @SerializedName("stripeSubscriptionId")
    private String stripeSubscriptionId;
    @SerializedName("token")
    private String token;
    @SerializedName("__v")
    private Integer v;
    @SerializedName("magicToken")
    private String magicToken;
    @SerializedName("tokenForPasswordReset")
    private String tokenForPasswordReset;
    @SerializedName("name")
    private String name;
    @SerializedName("phone")
    private String phone;
    @SerializedName("photo")
    private String photo;
    @SerializedName("tokenForPasswordResetIsFresh")
    private Boolean tokenForPasswordResetIsFresh;
    @SerializedName("webPushTokens")
    private List<String> webPushTokens = new ArrayList<>();
    @SerializedName("androidPushTokens")
    private List<String> androidPushTokens = new ArrayList<>();
    @SerializedName("iosPushTokens")
    private List<String> iosPushTokens = new ArrayList<>();
    @SerializedName("projects")
    private List<String> projects = new ArrayList<>();
    @SerializedName("addedAsManager")
    private Boolean addedAsManager;
    @SerializedName("addedAsClient")
    private Boolean addedAsClient;
    @SerializedName("isEmailVerified")
    private Boolean isEmailVerified;
    @SerializedName("isCompleted")
    private Boolean isCompleted;
    @SerializedName("isDemo")
    private Boolean isDemo;
    @SerializedName("isAdmin")
    private Boolean isAdmin;
    @SerializedName("isBusiness")
    private Boolean isBusiness;
    @SerializedName("numberOfActiveProjects")
    private Integer numberOfActiveProjects;
    @SerializedName("plan")
    private Integer plan;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStripeId() {
        return stripeId;
    }

    public void setStripeId(String stripeId) {
        this.stripeId = stripeId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getV() {
        return v;
    }

    public void setV(Integer v) {
        this.v = v;
    }

    public String getMagicToken() {
        return magicToken;
    }

    public void setMagicToken(String magicToken) {
        this.magicToken = magicToken;
    }

    public String getTokenForPasswordReset() {
        return tokenForPasswordReset;
    }

    public void setTokenForPasswordReset(String tokenForPasswordReset) {
        this.tokenForPasswordReset = tokenForPasswordReset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public Boolean getTokenForPasswordResetIsFresh() {
        return tokenForPasswordResetIsFresh;
    }

    public void setTokenForPasswordResetIsFresh(Boolean tokenForPasswordResetIsFresh) {
        this.tokenForPasswordResetIsFresh = tokenForPasswordResetIsFresh;
    }

    public List<String> getWebPushTokens() {
        return webPushTokens;
    }

    public void setWebPushTokens(List<String> webPushTokens) {
        this.webPushTokens = webPushTokens;
    }

    public List<String> getAndroidPushTokens() {
        return androidPushTokens;
    }

    public void setAndroidPushTokens(List<String> androidPushTokens) {
        this.androidPushTokens = androidPushTokens;
    }

    public List<String> getIosPushTokens() {
        return iosPushTokens;
    }

    public void setIosPushTokens(List<String> iosPushTokens) {
        this.iosPushTokens = iosPushTokens;
    }

    public List<String> getProjects() {
        return projects;
    }

    public void setProjects(List<String> projects) {
        this.projects = projects;
    }

    public Boolean getAddedAsManager() {
        return addedAsManager;
    }

    public void setAddedAsManager(Boolean addedAsManager) {
        this.addedAsManager = addedAsManager;
    }

    public Boolean getAddedAsClient() {
        return addedAsClient;
    }

    public void setAddedAsClient(Boolean addedAsClient) {
        this.addedAsClient = addedAsClient;
    }

    public Boolean getEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public Boolean getCompleted() {
        return isCompleted;
    }

    public void setCompleted(Boolean completed) {
        isCompleted = completed;
    }

    public Boolean getDemo() {
        return isDemo;
    }

    public void setDemo(Boolean demo) {
        isDemo = demo;
    }

    public Boolean getAdmin() {
        return isAdmin;
    }

    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    public Boolean getBusiness() {
        return isBusiness;
    }

    public void setBusiness(Boolean business) {
        isBusiness = business;
    }

    public Integer getNumberOfActiveProjects() {
        return numberOfActiveProjects;
    }

    public void setNumberOfActiveProjects(Integer numberOfActiveProjects) {
        this.numberOfActiveProjects = numberOfActiveProjects;
    }

    public Integer getPlan() {
        return plan;
    }

    public void setBusiness(Integer plan) {
        this.plan = plan;
    }

    @Override
    public String toString() {
        return "UserResponse{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", token='" + token + '\'' +
                ", v=" + v +
                ", magicToken='" + magicToken + '\'' +
                ", tokenForPasswordReset='" + tokenForPasswordReset + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", photo='" + photo + '\'' +
                ", tokenForPasswordResetIsFresh=" + tokenForPasswordResetIsFresh +
                ", webPushTokens=" + webPushTokens +
                ", androidPushTokens=" + androidPushTokens +
                ", iosPushTokens=" + iosPushTokens +
                ", projects=" + projects +
                ", addedAsManager=" + addedAsManager +
                ", addedAsClient=" + addedAsClient +
                ", isEmailVerified=" + isEmailVerified +
                ", isCompleted=" + isCompleted +
                ", isDemo=" + isDemo +
                ", isAdmin=" + isAdmin +
                ", isBusiness=" + isBusiness +
                ", numberOfActiveProjects=" + numberOfActiveProjects +
                ", plan=" + plan +
                '}';
    }
}