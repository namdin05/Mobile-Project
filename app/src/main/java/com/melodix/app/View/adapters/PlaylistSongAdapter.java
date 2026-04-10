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
import com.melodix.app.Utils.TimeUtils;   // ← Import này để format thời lượng

import java.util.List;

public class PlaylistSongAdapter extends RecyclerView.Adapter<PlaylistSongAdapter.ViewHolder> {

    private final Context context;
    private final List<PlaylistSong> playlistSongs;
    private final OnSongActionListener listener;

    public interface OnSongActionListener {
        void onSongClick(PlaylistSong playlistSong);
        void onMoreClick(PlaylistSong playlistSong, int position);
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
        if (playlistSong == null || playlistSong.song == null) return;

        Song song = playlistSong.song;

        String title = (song.getTitle() != null && !song.getTitle().trim().isEmpty())
                ? song.getTitle() : "Không có tiêu đề";

        String artist = (playlistSong.artistname != null && !playlistSong.artistname.trim().isEmpty())
                ? playlistSong.artistname : "Unknown Artist";

        holder.tvTitle.setText(title);
        holder.tvSubtitle.setText(artist);

        // Hiển thị thời lượng bài hát (KHÔNG phải songCount của playlist)
        if (song.getDurationSeconds() > 0) {
            holder.tvMeta.setText(TimeUtils.formatDuration(song.getDurationSeconds()));
        } else {
            holder.tvMeta.setText("--:--");
        }

        // Load ảnh bìa
        String coverUrl = song.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty() && !coverUrl.equals("null")) {
            Glide.with(context)
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.ic_music_placeholder);
        }

        // Click vào item → phát nhạc
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(playlistSong);
        });

        // Click nút More → hiện menu
        holder.btnMore.setOnClickListener(v -> {
            if (listener != null) listener.onMoreClick(playlistSong, position);
        });
    }

    @Override
    public int getItemCount() {
        return playlistSongs != null ? playlistSongs.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvSubtitle, tvMeta;   // tvMeta dùng để hiển thị thời lượng
        ImageButton btnMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvMeta = itemView.findViewById(R.id.tv_meta);           // ← Dùng tv_meta thay vì tv_song_count
            btnMore = itemView.findViewById(R.id.btn_more);
        }
    }
}