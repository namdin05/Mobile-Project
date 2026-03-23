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
import com.melodix.app.Model.Song;
import com.melodix.app.R;

import java.util.ArrayList;
import java.util.List;

public class TrendingSongAdapter extends RecyclerView.Adapter<TrendingSongAdapter.TrendingSongViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    private final List<Song> songs = new ArrayList<>();
    private final OnSongClickListener listener;

    public TrendingSongAdapter(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Song> newItems) {
        songs.clear();

        if (newItems != null) {
            songs.addAll(newItems);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrendingSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending_song, parent, false);
        return new TrendingSongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendingSongViewHolder holder, int position) {
        Song song = songs.get(position);

        holder.tvSongTitle.setText(song.getDisplayTitle());
        holder.tvSongSubtitle.setText(song.getDisplaySubtitle());

        try {
            Glide.with(holder.itemView.getContext())
                    .load(song.getCoverUrl())
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(holder.ivSongCover);
        } catch (Exception ignored) {
        }

        holder.cardSongRoot.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (listener != null) {
                listener.onSongClick(songs.get(adapterPosition), adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class TrendingSongViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardSongRoot;
        ImageView ivSongCover;
        TextView tvSongTitle;
        TextView tvSongSubtitle;

        public TrendingSongViewHolder(@NonNull View itemView) {
            super(itemView);
            cardSongRoot = itemView.findViewById(R.id.cardSongRoot);
            ivSongCover = itemView.findViewById(R.id.ivSongCover);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongSubtitle = itemView.findViewById(R.id.tvSongSubtitle);
        }
    }
}