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

import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Utils.TimeUtils;

import java.util.List;

public class PlaylistSongAdapter extends RecyclerView.Adapter<PlaylistSongAdapter.ViewHolder> {

    private final Context context;
    private final List<PlaylistSong> playlistSongs;
    private final OnPlaylistSongActionListener listener;

    public interface OnPlaylistSongActionListener {
        void onSongClick(PlaylistSong playlistSong);
        void onMoreClick(PlaylistSong playlistSong, int position);
    }


    public PlaylistSongAdapter(Context context, List<PlaylistSong> playlistSongs, OnPlaylistSongActionListener listener) {
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

        holder.tvTitle.setText(song.getTitle() != null ? song.getTitle() : "Không có tiêu đề");

        String artist = (playlistSong.artistname != null && !playlistSong.artistname.trim().isEmpty())
                ? playlistSong.artistname
                : (song.getArtistName() != null ? song.getArtistName() : "Unknown Artist");

        holder.tvSubtitle.setText(artist);

        if (song.getDurationSeconds() > 0) {
            holder.tvMeta.setText(TimeUtils.formatDuration(song.getDurationSeconds()));
        } else {
            holder.tvMeta.setText("--:--");
        }

        String coverUrl = song.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty() && !coverUrl.equals("null")) {
            com.bumptech.glide.Glide.with(context)
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.ic_music_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(playlistSong);
        });

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
        TextView tvTitle, tvSubtitle, tvMeta;
        ImageButton btnMore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            btnMore = itemView.findViewById(R.id.btn_more);
        }
    }
}