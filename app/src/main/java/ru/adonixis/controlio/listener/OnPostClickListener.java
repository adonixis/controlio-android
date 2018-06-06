package ru.adonixis.controlio.listener;

import android.view.View;

import ru.adonixis.controlio.model.UserResponse;

public interface OnPostClickListener {
    void onPostLongClick(View view, int position);
    void onManagerPhotoClick(View view, UserResponse manager);
}
