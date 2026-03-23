package com.melodix.app.View.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.melodix.app.Model.Genre;
import com.melodix.app.R;

import java.util.ArrayList;
import java.util.List;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {

    public interface OnGenreClickListener {
        void onGenreClick(Genre genre);
    }

    private final List<Genre> genres = new ArrayList<>();
    private final OnGenreClickListener listener;

    public GenreAdapter(OnGenreClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Genre> newItems) {
        genres.clear();

        if (newItems != null) {
            genres.addAll(newItems);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_genre, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        Genre genre = genres.get(position);

        holder.tvGenreName.setText(genre.getDisplayName());

        try {
            Glide.with(holder.itemView.getContext())
                    .load(genre.getImageUrl())
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(holder.ivGenreImage);
        } catch (Exception ignored) {
        }

        holder.cardGenreRoot.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (listener != null) {
                listener.onGenreClick(genres.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return genres.size();
    }

    static class GenreViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardGenreRoot;
        ImageView ivGenreImage;
        TextView tvGenreName;

        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            cardGenreRoot = itemView.findViewById(R.id.cardGenreRoot);
            ivGenreImage = itemView.findViewById(R.id.ivGenreImage);
            tvGenreName = itemView.findViewById(R.id.tvGenreName);
        }
    }
}