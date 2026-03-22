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
import com.google.android.material.card.MaterialCardView;
import com.melodix.app.Model.Song;
import com.melodix.app.R;

import java.util.Locale;

public class ArtistSongAdapter extends ListAdapter<Song, ArtistSongAdapter.SongViewHolder> {

    public static final int MODE_ARTIST_DETAIL = 1;
    public static final int MODE_ALBUM_DETAIL = 2;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    private String currentPlayingId = "";

    public void setCurrentPlayingId(String songId) {
        this.currentPlayingId = songId == null ? "" : songId;
        notifyDataSetChanged(); // Yêu cầu vẽ lại danh sách
    }

    private final OnSongClickListener listener;
    private final int displayMode;

    private static final DiffUtil.ItemCallback<Song> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Song>() {
                @Override
                public boolean areItemsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                    return safe(oldItem.getId()).equals(safe(newItem.getId()));
                }

                @Override
                public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                    return safe(oldItem.getTitle()).equals(safe(newItem.getTitle()))
                            && safe(oldItem.getAlbumTitle()).equals(safe(newItem.getAlbumTitle()))
                            && safe(oldItem.getArtistName()).equals(safe(newItem.getArtistName()))
                            && safe(oldItem.getCoverUrl()).equals(safe(newItem.getCoverUrl()))
                            && oldItem.getDurationMs() == newItem.getDurationMs()
                            && oldItem.getTrackNumber() == newItem.getTrackNumber()
                            && oldItem.isFavorite() == newItem.isFavorite();
                }
            };

    public ArtistSongAdapter(OnSongClickListener listener) {
        this(MODE_ARTIST_DETAIL, listener);
    }

    public ArtistSongAdapter(int displayMode, OnSongClickListener listener) {
        super(DIFF_CALLBACK);
        this.displayMode = displayMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_row, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        final Song song = getItem(position);

        holder.tvTrackNumber.setText(formatTrackNumber(song, position));
        if (song.getId().equals(currentPlayingId)) {
            holder.tvSongTitle.setTextColor(android.graphics.Color.parseColor("#1DB954"));
        } else {
            holder.tvSongTitle.setTextColor(android.graphics.Color.WHITE);
        }
        holder.tvSongSubtitle.setText(buildSubtitle(song));
        holder.tvSongDuration.setText(formatDuration(song.getDurationMs()));

        Glide.with(holder.itemView)
                .load(song.getCoverUrl())
                .thumbnail(0.2f)
                .centerCrop()
                .placeholder(new ColorDrawable(Color.parseColor("#20312B")))
                .error(new ColorDrawable(Color.parseColor("#20312B")))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.ivSongCover);

        holder.cardSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onSongClick(song);
                }
            }
        });
    }

    private String buildSubtitle(Song song) {
        if (displayMode == MODE_ALBUM_DETAIL) {
            if (!safe(song.getArtistName()).isEmpty()) {
                return safe(song.getArtistName());
            }
            return safe(song.getAlbumTitle());
        }

        if (!safe(song.getAlbumTitle()).isEmpty()) {
            return safe(song.getAlbumTitle());
        }
        return safe(song.getArtistName());
    }

    private String formatTrackNumber(Song song, int position) {
        int trackNumber = song.getTrackNumber() > 0 ? song.getTrackNumber() : position + 1;
        return String.format(Locale.getDefault(), "%02d", trackNumber);
    }

    private String formatDuration(long durationMs) {
        long totalSeconds = Math.max(0L, durationMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardSong;
        private final TextView tvTrackNumber;
        private final ImageView ivSongCover;
        private final TextView tvSongTitle;
        private final TextView tvSongSubtitle;
        private final TextView tvSongDuration;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            cardSong = itemView.findViewById(R.id.cardSong);
            tvTrackNumber = itemView.findViewById(R.id.tvTrackNumber);
            ivSongCover = itemView.findViewById(R.id.ivSongCover);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongSubtitle = itemView.findViewById(R.id.tvSongSubtitle);
            tvSongDuration = itemView.findViewById(R.id.tvSongDuration);
        }
    }
}