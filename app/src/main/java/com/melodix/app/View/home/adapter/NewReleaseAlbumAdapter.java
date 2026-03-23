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
import com.melodix.app.Model.Album;
import com.melodix.app.R;

import java.util.ArrayList;
import java.util.List;

public class NewReleaseAlbumAdapter extends RecyclerView.Adapter<NewReleaseAlbumAdapter.AlbumViewHolder> {

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    private final List<Album> albums = new ArrayList<>();
    private final OnAlbumClickListener listener;

    public NewReleaseAlbumAdapter(OnAlbumClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Album> newItems) {
        albums.clear();

        if (newItems != null) {
            albums.addAll(newItems);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);

        holder.tvAlbumTitle.setText(album.getDisplayTitle());
        holder.tvAlbumSubtitle.setText(album.getDisplaySubtitle());

        try {
            Glide.with(holder.itemView.getContext())
                    .load(album.getCoverUrl())
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(holder.ivAlbumCover);
        } catch (Exception ignored) {
        }

        holder.cardAlbumRoot.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (listener != null) {
                listener.onAlbumClick(albums.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardAlbumRoot;
        ImageView ivAlbumCover;
        TextView tvAlbumTitle;
        TextView tvAlbumSubtitle;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAlbumRoot = itemView.findViewById(R.id.cardAlbumRoot);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvAlbumTitle = itemView.findViewById(R.id.tvAlbumTitle);
            tvAlbumSubtitle = itemView.findViewById(R.id.tvAlbumSubtitle);
        }
    }
}