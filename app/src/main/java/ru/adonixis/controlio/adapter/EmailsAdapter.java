package ru.adonixis.controlio.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.adonixis.controlio.databinding.ItemEmailBinding;

public class EmailsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<String> emails;

    public EmailsAdapter(List<String> emails) {
        this.emails = emails;
    }

    class EmailItemViewHolder extends RecyclerView.ViewHolder {

        private ItemEmailBinding itemEmailBinding;

        EmailItemViewHolder(View v) {
            super(v);
            itemEmailBinding = DataBindingUtil.bind(v);
        }

        public ItemEmailBinding getBinding() {
            return itemEmailBinding;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemEmailBinding itemEmailBinding = ItemEmailBinding.inflate(inflater, parent, false);
        return new EmailItemViewHolder(itemEmailBinding.getRoot());
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        String email = emails.get(position);
        EmailItemViewHolder emailItemViewHolder = (EmailItemViewHolder) viewHolder;
        emailItemViewHolder.getBinding().setEmail(email);
        emailItemViewHolder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return emails == null ? 0 : emails.size();
    }

}