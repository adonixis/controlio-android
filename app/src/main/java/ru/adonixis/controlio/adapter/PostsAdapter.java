package ru.adonixis.controlio.adapter;

import android.app.Dialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

import ru.adonixis.controlio.R;
import ru.adonixis.controlio.databinding.DialogFullImageBinding;
import ru.adonixis.controlio.databinding.ItemPostBinding;
import ru.adonixis.controlio.listener.OnAttachmentClickListener;
import ru.adonixis.controlio.listener.OnPostClickListener;
import ru.adonixis.controlio.model.PostDetailsResponse;
import ru.adonixis.controlio.model.ProjectResponse;

public class PostsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private DialogFullImageBinding dialogFullImageBinding;
    private final List<PostDetailsResponse> posts;
    private final ProjectResponse project;
    private final OnPostClickListener onPostClickListener;

    public PostsAdapter(List<PostDetailsResponse> posts, ProjectResponse project, OnPostClickListener onPostClickListener) {
        this.posts = posts;
        this.project = project;
        this.onPostClickListener = onPostClickListener;
    }

    class PostItemViewHolder extends RecyclerView.ViewHolder {

        private ItemPostBinding itemPostBinding;

        PostItemViewHolder(View v) {
            super(v);
            itemPostBinding = DataBindingUtil.bind(v);

            if (project.isCanEdit() && !project.isFinished()) {
                itemPostBinding.rootLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        onPostClickListener.onPostLongClick(view, getAdapterPosition());
                        return true;
                    }
                });
            }

            itemPostBinding.imageManager.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onPostClickListener.onManagerPhotoClick(view, posts.get(getAdapterPosition()).getAuthor());
                }
            });
        }

        public ItemPostBinding getBinding() {
            return itemPostBinding;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemPostBinding itemPostBinding = ItemPostBinding.inflate(inflater, parent, false);
        return new PostItemViewHolder(itemPostBinding.getRoot());
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position) {
        PostDetailsResponse post = posts.get(viewHolder.getAdapterPosition());
        PostItemViewHolder postItemViewHolder = (PostItemViewHolder) viewHolder;
        postItemViewHolder.getBinding().setPost(post);

        final Context context = postItemViewHolder.getBinding().getRoot().getContext();

        final List<String> attachments = post.getAttachments();
        if (!attachments.isEmpty()) {
            final RecyclerView recyclerAttachments = postItemViewHolder.getBinding().recyclerAttachments;
            LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            recyclerAttachments.setLayoutManager(layoutManager);
            recyclerAttachments.setHasFixedSize(true);
            final AttachmentsAdapter attachmentsAdapter = new AttachmentsAdapter(attachments, new OnAttachmentClickListener() {
                @Override
                public void onAttachmentClick(View view, int position) {
                    dialogFullImageBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_full_image, null, false);
                    String attachment = attachments.get(position);

                    File file = new File(context.getCacheDir(), '/' + attachment);
                    if (file.exists()) {
                        Glide.with(context)
                                .load(file)
                                .into(dialogFullImageBinding.imageFull);

                        final Dialog dialog = new Dialog(context, R.style.AppTheme_Light_DialogTransparent);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.setContentView(dialogFullImageBinding.getRoot());
                        Window window = dialog.getWindow();
                        if (window != null) {
                            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        }
                        dialog.show();
                        dialogFullImageBinding.imageFull.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.cancel();
                            }
                        });
                    }
                }
            });
            recyclerAttachments.setAdapter(attachmentsAdapter);
        }
        postItemViewHolder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return posts == null ? 0 : posts.size();
    }

}