package ru.adonixis.controlio.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.adonixis.controlio.databinding.ItemUserBinding;
import ru.adonixis.controlio.listener.OnUserClickListener;
import ru.adonixis.controlio.model.UserResponse;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserItemViewHolder> {

    private final List<UserResponse> users;
    private final OnUserClickListener onUserClickListener;

    public UsersAdapter(List<UserResponse> users, OnUserClickListener onUserClickListener) {
        this.users = users;
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
                    onUserClickListener.onUserClick(v, users.get(getAdapterPosition()));
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
        UserResponse user = users.get(position);
        userItemViewHolder.getBinding().setUser(user);
        userItemViewHolder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return users == null ? 0 : users.size();
    }

}