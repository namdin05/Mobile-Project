package com.melodix.app.View.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.melodix.app.Model.Song;
import com.melodix.app.R;

import java.util.List;

public class SongCardAdapter extends RecyclerView.Adapter<SongCardAdapter.SongHolder> {
    Context context;
    List<Song> songs;
    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    private final OnSongClickListener listener;
    private final boolean showRanking;

    public SongCardAdapter(Context context, List<Song> songs, boolean showRanking, OnSongClickListener listener){
        this.context = context;
        this.showRanking = showRanking;
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongCardAdapter.SongHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SongCardAdapter.SongHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SongCardAdapter.SongHolder holder, int position) {
        Song song = songs.get(position);
        Glide.with(context).load(song.getCover_url()).into(holder.image);
        holder.title.setText(song.getTitle());
        holder.badge.setVisibility(showRanking ? View.VISIBLE : View.GONE);
        holder.badge.setText(String.valueOf(position + 1));
        holder.itemView.setOnClickListener(v -> {
            if(listener != null) listener.onSongClick(song);
        });
    }

    @Override
    public int getItemCount() { return songs.size(); }

    static class SongHolder extends RecyclerView.ViewHolder {
        ImageView image; TextView title, badge, artist;
        SongHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.img_cover);
            title = itemView.findViewById(R.id.tv_title);
            badge = itemView.findViewById(R.id.tv_badge);
            artist = itemView.findViewById(R.id.tv_subtitle);
        }
    }
}
