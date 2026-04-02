package com.melodix.app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.PlaylistSong;
import com.melodix.app.R;

import java.util.ArrayList;
import java.util.List;

public class PlaylistSongAdapter extends RecyclerView.Adapter<PlaylistSongAdapter.ViewHolder> {

    private List<PlaylistSong> songList = new ArrayList<>();
    private OnSongActionListener listener;

    public interface OnSongActionListener {
        void onRemoveClick(PlaylistSong playlistSong);
    }

    public PlaylistSongAdapter(OnSongActionListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<PlaylistSong> songs) {
        this.songList.clear();
        if (songs != null) {
            this.songList.addAll(songs);
        }
        notifyDataSetChanged();
    }

    // Drag & Drop - Di chuyển item
    public void moveItem(int fromPosition, int toPosition) {
        PlaylistSong item = songList.remove(fromPosition);
        songList.add(toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    // Trả về danh sách hiện tại để cập nhật thứ tự lên server
    public List<PlaylistSong> getCurrentOrder() {
        return new ArrayList<>(songList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaylistSong playlistSong = songList.get(position);
        com.melodix.app.Model.Song song = playlistSong.getSong();

        if (song != null) {
            holder.tvSongTitle.setText(song.getTitle());
        } else {
            holder.tvSongTitle.setText("Unknown Song");
        }

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveClick(playlistSong);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongTitle;
        ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}