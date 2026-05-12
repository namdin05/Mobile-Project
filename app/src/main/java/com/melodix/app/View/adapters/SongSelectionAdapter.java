package com.melodix.app.View.adapters;

import android.content.Context;
import android.graphics.Color;
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

import java.util.ArrayList;
import java.util.List;

public class SongSelectionAdapter extends RecyclerView.Adapter<SongSelectionAdapter.ViewHolder> {

    private Context context;
    private List<Song> songs;
    private List<String> selectedSongIds; 

    public SongSelectionAdapter(Context context, List<Song> songs, List<String> selectedSongIds) {
        this.context = context;
        this.songs = songs != null ? songs : new ArrayList<>();
        this.selectedSongIds = selectedSongIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);

        holder.tvTitle.setText(song.getTitle());

        
        holder.tvArtist.setText(song.getArtistName() != null ? song.getArtistName() : "Nghệ sĩ");

        Glide.with(context)
                .load(song.getCoverUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imgCover);

        
        if (holder.btnMore != null) {
            holder.btnMore.setVisibility(View.GONE);
        }

        
        if (selectedSongIds.contains(song.getId())) {
            
            holder.itemView.setBackgroundColor(Color.parseColor("#331DB954"));
        } else {
            
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        
        holder.itemView.setOnClickListener(v -> {
            if (selectedSongIds.contains(song.getId())) {
                selectedSongIds.remove(song.getId()); 
            } else {
                selectedSongIds.add(song.getId()); 
            }

            
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public void updateData(List<Song> newSongs) {
        this.songs.clear();
        this.songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover, btnMore;
        TextView tvTitle, tvArtist;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            
            imgCover = itemView.findViewById(R.id.img_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_subtitle);
            btnMore = itemView.findViewById(R.id.btn_more);   
        }
    }
}