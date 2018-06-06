package ru.adonixis.controlio.adapter;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import ru.adonixis.controlio.databinding.ItemNewAttachmentBinding;
import ru.adonixis.controlio.listener.OnNewAttachmentClickListener;
import ru.adonixis.controlio.util.CropSquareTransformation;
import ru.adonixis.controlio.util.RoundedCornersTransformation;

public class NewAttachmentsAdapter extends RecyclerView.Adapter<NewAttachmentsAdapter.NewAttachmentItemViewHolder> {

    private static final int DOWNLOADED_ATTACHMENT_VIEW_TYPE = 1;
    private final List<String> downloadedAttachments;
    private final List<Uri> newAttachments;
    private final OnNewAttachmentClickListener onNewAttachmentClickListener;

    public NewAttachmentsAdapter(List<String> downloadedAttachments, List<Uri> newAttachments, OnNewAttachmentClickListener onNewAttachmentClickListener) {
        this.downloadedAttachments = downloadedAttachments;
        this.newAttachments = newAttachments;
        this.onNewAttachmentClickListener = onNewAttachmentClickListener;
    }

    class NewAttachmentItemViewHolder extends RecyclerView.ViewHolder {

        private ItemNewAttachmentBinding itemNewAttachmentBinding;

        NewAttachmentItemViewHolder(View v) {
            super(v);
            itemNewAttachmentBinding = DataBindingUtil.bind(v);
        }

        public ItemNewAttachmentBinding getBinding() {
            return itemNewAttachmentBinding;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < downloadedAttachments.size()) {
            return DOWNLOADED_ATTACHMENT_VIEW_TYPE;
        }
        return super.getItemViewType(position);
    }

    @Override
    public NewAttachmentItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemNewAttachmentBinding itemNewAttachmentBinding = ItemNewAttachmentBinding.inflate(inflater, parent, false);
        return new NewAttachmentItemViewHolder(itemNewAttachmentBinding.getRoot());
    }

    @Override
    public void onBindViewHolder(final NewAttachmentItemViewHolder newAttachmentItemViewHolder, int position) {
        if (getItemViewType(position) == DOWNLOADED_ATTACHMENT_VIEW_TYPE) {
            newAttachmentItemViewHolder.getBinding().imageNewAttachment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNewAttachmentClickListener.onDownloadedAttachmentClick(v, newAttachmentItemViewHolder.getAdapterPosition());
                }
            });
            newAttachmentItemViewHolder.getBinding().iconCross.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNewAttachmentClickListener.onRemoveDownloadedAttachmentClick(v, newAttachmentItemViewHolder.getAdapterPosition());
                }
            });

            newAttachmentItemViewHolder.getBinding().setAttachment(downloadedAttachments.get(position));
        } else {
            newAttachmentItemViewHolder.getBinding().imageNewAttachment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNewAttachmentClickListener.onNewAttachmentClick(v, newAttachmentItemViewHolder.getAdapterPosition() - downloadedAttachments.size());
                }
            });
            newAttachmentItemViewHolder.getBinding().iconCross.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNewAttachmentClickListener.onRemoveNewAttachmentClick(v, newAttachmentItemViewHolder.getAdapterPosition() - downloadedAttachments.size());
                }
            });

            Uri newAttachment = newAttachments.get(position - downloadedAttachments.size());
            Context context = newAttachmentItemViewHolder.getBinding().getRoot().getContext();
            Glide.with(context)
                    .load(newAttachment)
                    .apply(RequestOptions.bitmapTransform(new CropSquareTransformation(context)))
                    .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(context, 20, 0)))
                    .into(newAttachmentItemViewHolder.getBinding().imageNewAttachment);
        }

        newAttachmentItemViewHolder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return downloadedAttachments.size() + newAttachments.size();
    }

}