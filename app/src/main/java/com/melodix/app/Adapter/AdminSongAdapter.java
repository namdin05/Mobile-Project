package com.melodix.app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import java.util.List;

public class AdminSongAdapter extends RecyclerView.Adapter<AdminSongAdapter.ViewHolder> {

    private List<Song> songList;

    public AdminSongAdapter(List<Song> songList) {
        this.songList = songList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.tvSongTitle.setText(song.getTitle());
        holder.tvArtistName.setText(song.getArtistName());
        holder.tvGenreTag.setText(song.getGenre());
        holder.tvPlayCount.setText("▶ " + song.getPlayCount());
    }

    @Override
    public int getItemCount() { return songList != null ? songList.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSongTitle, tvArtistName, tvGenreTag, tvPlayCount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            tvGenreTag = itemView.findViewById(R.id.tvGenreTag);
            tvPlayCount = itemView.findViewById(R.id.tvPlayCount);
        }
    }
}