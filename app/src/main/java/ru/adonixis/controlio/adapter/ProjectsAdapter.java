package ru.adonixis.controlio.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.adonixis.controlio.databinding.ItemInviteBinding;
import ru.adonixis.controlio.databinding.ItemProjectBinding;
import ru.adonixis.controlio.listener.OnInviteClickListener;
import ru.adonixis.controlio.listener.OnProjectClickListener;
import ru.adonixis.controlio.model.InviteDetailsResponse;
import ru.adonixis.controlio.model.ProjectResponse;

public class ProjectsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int INVITE_VIEW = 1;
    private final List<ProjectResponse> projects;
    private final List<InviteDetailsResponse> invites;
    private final OnProjectClickListener onProjectClickListener;
    private final OnInviteClickListener onInviteClickListener;

    public ProjectsAdapter(List<ProjectResponse> projects, List<InviteDetailsResponse> invites, OnProjectClickListener onProjectClickListener, OnInviteClickListener onInviteClickListener) {
        this.projects = projects;
        this.invites = invites;
        this.onProjectClickListener = onProjectClickListener;
        this.onInviteClickListener = onInviteClickListener;
    }

    private class InviteItemViewHolder extends RecyclerView.ViewHolder {

        private ItemInviteBinding itemInviteBinding;

        InviteItemViewHolder(View v) {
            super(v);
            itemInviteBinding = DataBindingUtil.bind(v);

            itemInviteBinding.iconAccept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onInviteClickListener.onAcceptClick(view, invites.get(getAdapterPosition()));
                }
            });

            itemInviteBinding.iconReject.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onInviteClickListener.onRejectClick(view, invites.get(getAdapterPosition()));
                }
            });
        }

        public ItemInviteBinding getBinding() {
            return itemInviteBinding;
        }
    }

    private class ProjectItemViewHolder extends RecyclerView.ViewHolder {

        private ItemProjectBinding mItemProjectBinding;

        ProjectItemViewHolder(View v) {
            super(v);
            mItemProjectBinding = DataBindingUtil.bind(v);
            mItemProjectBinding.rootLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onProjectClickListener.onProjectClick(view, getAdapterPosition() - invites.size());
                }
            });
        }

        public ItemProjectBinding getBinding() {
            return mItemProjectBinding;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < invites.size()) {
            return INVITE_VIEW;
        }
        return super.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == INVITE_VIEW) {
            ItemInviteBinding itemInviteBinding = ItemInviteBinding.inflate(inflater, parent, false);
            return new InviteItemViewHolder(itemInviteBinding.getRoot());
        }
        ItemProjectBinding itemProjectBinding = ItemProjectBinding.inflate(inflater, parent, false);
        return new ProjectItemViewHolder(itemProjectBinding.getRoot());
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (getItemViewType(position) == INVITE_VIEW) {
            InviteDetailsResponse invite = invites.get(position);
            InviteItemViewHolder inviteItemViewHolder = (InviteItemViewHolder) viewHolder;
            inviteItemViewHolder.getBinding().setInvite(invite);
            inviteItemViewHolder.getBinding().executePendingBindings();
        } else {
            ProjectResponse project = projects.get(position - invites.size());
            ProjectItemViewHolder projectItemViewHolder = (ProjectItemViewHolder) viewHolder;
            projectItemViewHolder.getBinding().setProject(project);
            projectItemViewHolder.getBinding().executePendingBindings();
        }
    }

    @Override
    public int getItemCount() {
        return invites.size() + projects.size();
    }
}
