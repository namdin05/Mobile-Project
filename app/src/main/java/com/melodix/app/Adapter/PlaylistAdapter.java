package com.melodix.app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

//import com.bumptech.glide.Glide;
import com.melodix.app.Model.Playlist;
import com.melodix.app.R;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private List<Playlist> playlistList;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
    }

    public PlaylistAdapter(OnPlaylistClickListener listener) {
        this.listener = listener;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlistList = playlists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlistList.get(position);

        holder.tvPlaylistName.setText(playlist.getName());

//        // Load ảnh bìa nếu có
//        if (playlist.getCoverUrl() != null && !playlist.getCoverUrl().isEmpty()) {
//            Glide.with(holder.itemView.getContext())
//                    .load(playlist.getCoverUrl())
//                    .placeholder(R.drawable.ic_music_placeholder)
//                    .into(holder.imgCover);
//        } else {
//            holder.imgCover.setImageResource(R.drawable.ic_music_placeholder);
//        }
        holder.imgCover.setImageResource(R.drawable.ic_music_placeholder);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlistList != null ? playlistList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvPlaylistName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
        }
    }
}