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
import com.melodix.app.Model.Artist;
import com.melodix.app.R;

public class RelatedArtistAdapter extends ListAdapter<Artist, RelatedArtistAdapter.ArtistViewHolder> {

    public interface OnArtistClickListener { void onArtistClick(Artist artist); }
    private final OnArtistClickListener listener;

    public RelatedArtistAdapter(OnArtistClickListener listener) {
        super(new DiffUtil.ItemCallback<Artist>() {
            @Override public boolean areItemsTheSame(@NonNull Artist o, @NonNull Artist n) { return o.getId().equals(n.getId()); }
            @Override public boolean areContentsTheSame(@NonNull Artist o, @NonNull Artist n) { return o.getName().equals(n.getName()); }
        });
        this.listener = listener;
    }

    @NonNull @Override public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ArtistViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_horizontal, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = getItem(position);
        holder.tvArtistName.setText(artist.getName());
        Glide.with(holder.itemView).load(artist.getImageUrl()).thumbnail(0.2f).centerCrop()
                .placeholder(new ColorDrawable(Color.parseColor("#20312B")))
                .transition(DrawableTransitionOptions.withCrossFade()).into(holder.ivArtistAvatar);
        holder.itemView.setOnClickListener(v -> { if(listener != null) listener.onArtistClick(artist); });
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ImageView ivArtistAvatar; TextView tvArtistName;
        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtistAvatar = itemView.findViewById(R.id.ivArtistAvatar);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
        }
    }
}