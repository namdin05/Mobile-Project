package com.melodix.app.View.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import java.util.List;

public class PlaylistSongAdapter extends RecyclerView.Adapter<PlaylistSongAdapter.ViewHolder> {

    private final Context context;
    private final List<PlaylistSong> playlistSongs;
    private final OnSongActionListener listener;

    public interface OnSongActionListener {
        void onSongClick(PlaylistSong playlistSong);
    }

    public PlaylistSongAdapter(Context context, List<PlaylistSong> playlistSongs, OnSongActionListener listener) {
        this.context = context;
        this.playlistSongs = playlistSongs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaylistSong playlistSong = playlistSongs.get(position);

        if (playlistSong == null || playlistSong.song == null) {
            return;
        }

        Song song = playlistSong.song;

        // Title
        String title = song.getTitle();
        if (title == null || title.trim().isEmpty() || title.equals("null")) {
            title = "Không có tiêu đề";
        }

        // Artist - LẤY TRỰC TIẾP TỪ artistname CỦA PLAYLISTSONG
        String artist = playlistSong.artistname;
        if (artist == null || artist.trim().isEmpty() || artist.equals("null")) {
            artist = "Unknown Artist";
        }

        // Cover URL
        String coverUrl = song.getCoverUrl();

        holder.tvTitle.setText(title);
        holder.tvSubtitle.setText(artist);

        // Load ảnh
        if (coverUrl != null && !coverUrl.isEmpty() && !coverUrl.equals("null")) {
            Glide.with(context)
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.ic_music_placeholder);
        }

        // Sự kiện click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(playlistSong);
            }
        });

        // Nút xóa
//        holder.btnRemove.setOnClickListener(v -> {
//            if (listener != null) {
//                listener.onSongRemove(playlistSong);
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return playlistSongs != null ? playlistSongs.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvSubtitle;
//        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
        }
    }
}