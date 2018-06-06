package ru.adonixis.controlio.adapter;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import ru.adonixis.controlio.databinding.ItemCardBinding;
import ru.adonixis.controlio.listener.OnCardClickListener;
import ru.adonixis.controlio.model.StripeSourceResponse;

public class CardsAdapter extends RecyclerView.Adapter<CardsAdapter.CardItemViewHolder> {

    private final List<StripeSourceResponse> cards;
    private final OnCardClickListener onCardClickListener;

    public CardsAdapter(List<StripeSourceResponse> cards, OnCardClickListener onCardClickListener) {
        this.cards = cards;
        this.onCardClickListener = onCardClickListener;
    }

    class CardItemViewHolder extends RecyclerView.ViewHolder {

        private ItemCardBinding itemCardBinding;

        CardItemViewHolder(View v) {
            super(v);
            itemCardBinding = DataBindingUtil.bind(v);
            itemCardBinding.rootLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onCardClickListener.onCardClick(view, getAdapterPosition());
                }
            });
        }

        public ItemCardBinding getBinding() {
            return itemCardBinding;
        }
    }

    @Override
    public CardItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemCardBinding itemCardBinding = ItemCardBinding.inflate(inflater, parent, false);
        return new CardItemViewHolder(itemCardBinding.getRoot());
    }

    @Override
    public void onBindViewHolder(CardItemViewHolder cardItemViewHolder, int position) {
        StripeSourceResponse card = cards.get(position);
        cardItemViewHolder.getBinding().setCard(card);
        cardItemViewHolder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return cards == null ? 0 : cards.size();
    }

}