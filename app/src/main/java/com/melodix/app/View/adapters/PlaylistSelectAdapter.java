package com.melodix.app.View.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.melodix.app.Model.Playlist;
import com.melodix.app.R;
import java.util.List;

public class PlaylistSelectAdapter extends RecyclerView.Adapter<PlaylistSelectAdapter.ViewHolder> {

    private final Context context;
    private final List<Playlist> playlists;
    private final List<String> selectedPlaylistIds;
    private final OnPlaylistSelectListener listener;

    public interface OnPlaylistSelectListener {
        void onPlaylistClick(Playlist playlist, boolean isSelected);
    }

    public PlaylistSelectAdapter(Context context, List<Playlist> playlists,
                                 List<String> selectedPlaylistIds,
                                 OnPlaylistSelectListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.selectedPlaylistIds = selectedPlaylistIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);

        boolean isSelected = selectedPlaylistIds != null && selectedPlaylistIds.contains(playlist.id);

        holder.tvName.setText(playlist.name);
        holder.tvSongCount.setText(playlist.songCount + " bài hát");
        holder.checkSelected.setChecked(isSelected);

        if (playlist.coverRes != null && !playlist.coverRes.isEmpty()) {
            Glide.with(context)
                    .load(playlist.coverRes)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.ic_music_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            boolean newState = !holder.checkSelected.isChecked();
            holder.checkSelected.setChecked(newState);
            if (listener != null) {
                listener.onPlaylistClick(playlist, newState);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists != null ? playlists.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgCover;
        TextView tvName, tvSongCount;
        CheckBox checkSelected;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_cover);
            tvName = itemView.findViewById(R.id.tv_name);
            tvSongCount = itemView.findViewById(R.id.tv_song_count);
            checkSelected = itemView.findViewById(R.id.check_selected);
        }
    }
}