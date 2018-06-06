package ru.adonixis.controlio.listener;

import android.view.View;

import ru.adonixis.controlio.model.UserResponse;

public interface OnUserClickListener {
    void onUserClick(View view, UserResponse user);
}