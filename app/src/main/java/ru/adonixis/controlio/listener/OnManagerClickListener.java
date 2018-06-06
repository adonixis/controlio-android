package ru.adonixis.controlio.listener;

import android.view.View;

public interface OnManagerClickListener {
    void onManagerClick(View view, int position);
    void onAddManagerClick(View view, int position);
    void onManagerPhotoClick(View view, int position);
}
