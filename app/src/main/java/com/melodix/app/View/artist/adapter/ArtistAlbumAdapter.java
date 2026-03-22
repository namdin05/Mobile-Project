package com.melodix.app.View.artist.adapter;

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
import com.melodix.app.Model.Album;
import com.melodix.app.R;

public class ArtistAlbumAdapter extends ListAdapter<Album, ArtistAlbumAdapter.AlbumViewHolder> {

    public interface OnAlbumClickListener { void onAlbumClick(Album album); }
    private final OnAlbumClickListener listener;

    public ArtistAlbumAdapter(OnAlbumClickListener listener) {
        super(new DiffUtil.ItemCallback<Album>() {
            @Override public boolean areItemsTheSame(@NonNull Album o, @NonNull Album n) { return o.getId().equals(n.getId()); }
            @Override public boolean areContentsTheSame(@NonNull Album o, @NonNull Album n) { return o.getTitle().equals(n.getTitle()); }
        });
        this.listener = listener;
    }

    @NonNull @Override public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AlbumViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_horizontal, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = getItem(position);
        holder.tvAlbumTitle.setText(album.getTitle());
        holder.tvAlbumYear.setText(album.getReleaseDate() != null && album.getReleaseDate().length() >= 4 ? album.getReleaseDate().substring(0, 4) : "");
        Glide.with(holder.itemView).load(album.getCoverUrl()).thumbnail(0.2f).centerCrop()
                .placeholder(new ColorDrawable(Color.parseColor("#20312B")))
                .transition(DrawableTransitionOptions.withCrossFade()).into(holder.ivAlbumCover);
        holder.itemView.setOnClickListener(v -> { if(listener != null) listener.onAlbumClick(album); });
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumCover; TextView tvAlbumTitle, tvAlbumYear;
        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvAlbumTitle = itemView.findViewById(R.id.tvAlbumTitle);
            tvAlbumYear = itemView.findViewById(R.id.tvAlbumYear);
        }
    }
}