package ru.adonixis.controlio.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.adonixis.controlio.databinding.ItemUserBinding;
import ru.adonixis.controlio.listener.OnUserClickListener;
import ru.adonixis.controlio.model.InviteResponse;

public class InvitesAdapter extends RecyclerView.Adapter<InvitesAdapter.UserItemViewHolder> {

    private final List<InviteResponse> invites;
    private final OnUserClickListener onUserClickListener;

    public InvitesAdapter(List<InviteResponse> invites, OnUserClickListener onUserClickListener) {
        this.invites = invites;
        this.onUserClickListener = onUserClickListener;
    }

    class UserItemViewHolder extends RecyclerView.ViewHolder {

        private ItemUserBinding itemUserBinding;

        UserItemViewHolder(View v) {
            super(v);
            itemUserBinding = DataBindingUtil.bind(v);
            itemUserBinding.rootLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onUserClickListener.onUserClick(v, invites.get(getAdapterPosition()).getInvitee());
                }
            });
        }

        public ItemUserBinding getBinding() {
            return itemUserBinding;
        }
    }

    @Override
    public UserItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemUserBinding itemEmailBinding = ItemUserBinding.inflate(inflater, parent, false);
        return new UserItemViewHolder(itemEmailBinding.getRoot());
    }

    @Override
    public void onBindViewHolder(UserItemViewHolder userItemViewHolder, final int position) {
        InviteResponse invite = invites.get(position);
        userItemViewHolder.getBinding().setUser(invite.getInvitee());
        userItemViewHolder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return invites == null ? 0 : invites.size();
    }

}