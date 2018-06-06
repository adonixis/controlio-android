package ru.adonixis.controlio.model;

public class EditProfileRequest {
    private final String name;
    private final String phone;
    private final String photo;

    public EditProfileRequest(String name, String phone, String photo) {
        this.name = name;
        this.phone = phone;
        this.photo = photo;
    }
}