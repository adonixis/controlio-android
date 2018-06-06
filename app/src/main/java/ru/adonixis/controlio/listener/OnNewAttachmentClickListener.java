package ru.adonixis.controlio.listener;

import android.view.View;

public interface OnNewAttachmentClickListener {
    void onNewAttachmentClick(View view, int position);
    void onRemoveNewAttachmentClick(View view, int position);
    void onDownloadedAttachmentClick(View view, int position);
    void onRemoveDownloadedAttachmentClick(View view, int position);
}
