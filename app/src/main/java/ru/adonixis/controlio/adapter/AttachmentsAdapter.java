package ru.adonixis.controlio.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.adonixis.controlio.databinding.ItemAttachmentBinding;
import ru.adonixis.controlio.listener.OnAttachmentClickListener;

public class AttachmentsAdapter extends RecyclerView.Adapter<AttachmentsAdapter.AttachmentItemViewHolder> {

    private final List<String> attachments;
    private final OnAttachmentClickListener onAttachmentClickListener;

    public AttachmentsAdapter(List<String> attachments, OnAttachmentClickListener onAttachmentClickListener) {
        this.attachments = attachments;
        this.onAttachmentClickListener = onAttachmentClickListener;
    }

    class AttachmentItemViewHolder extends RecyclerView.ViewHolder {

        private ItemAttachmentBinding itemAttachmentBinding;

        AttachmentItemViewHolder(View v) {
            super(v);
            itemAttachmentBinding = DataBindingUtil.bind(v);
            itemAttachmentBinding.imageAttachment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onAttachmentClickListener.onAttachmentClick(v, getAdapterPosition());
                }
            });
        }

        public ItemAttachmentBinding getBinding() {
            return itemAttachmentBinding;
        }
    }

    @Override
    public AttachmentItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAttachmentBinding itemAttachmentBinding = ItemAttachmentBinding.inflate(inflater, parent, false);
        return new AttachmentItemViewHolder(itemAttachmentBinding.getRoot());
    }

    @Override
    public void onBindViewHolder(AttachmentItemViewHolder attachmentItemViewHolder, final int position) {
        String attachment = attachments.get(position);
        attachmentItemViewHolder.getBinding().setAttachment(attachment);
        attachmentItemViewHolder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return attachments == null ? 0 : attachments.size();
    }

}