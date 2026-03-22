package com.melodix.app.View.search.adapter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.card.MaterialCardView;
import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.R;

public class SearchResultAdapter extends ListAdapter<SearchResultItem, SearchResultAdapter.SearchResultViewHolder> {

    private static final DiffUtil.ItemCallback<SearchResultItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SearchResultItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull SearchResultItem oldItem,
                                               @NonNull SearchResultItem newItem) {
                    return oldItem.getType() == newItem.getType()
                            && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull SearchResultItem oldItem,
                                                  @NonNull SearchResultItem newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getSubtitle().equals(newItem.getSubtitle())
                            && oldItem.getImageUrl().equals(newItem.getImageUrl());
                }
            };

    private final OnSearchResultClickListener listener;

    public interface OnSearchResultClickListener {
        void onSearchResultClick(SearchResultItem item);
    }

    public SearchResultAdapter(OnSearchResultClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        final SearchResultItem item = getItem(position);

        holder.tvResultTitle.setText(item.getTitle());
        holder.tvResultSubtitle.setText(item.getSubtitle());
        holder.ivCover.setContentDescription(item.getTitle());

        Glide.with(holder.itemView)
                .load(item.getImageUrl())
                .centerCrop()
                .placeholder(new ColorDrawable(Color.parseColor("#20312B")))
                .error(new ColorDrawable(Color.parseColor("#20312B")))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.ivCover);

        holder.cardResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onSearchResultClick(item);
                }
            }
        });
    }

    static class SearchResultViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardResult;
        private final ImageView ivCover;
        private final TextView tvResultTitle;
        private final TextView tvResultSubtitle;

        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            cardResult = itemView.findViewById(R.id.cardResult);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvResultTitle = itemView.findViewById(R.id.tvResultTitle);
            tvResultSubtitle = itemView.findViewById(R.id.tvResultSubtitle);
        }
    }
}