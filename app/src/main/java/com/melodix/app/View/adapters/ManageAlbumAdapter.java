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
import com.melodix.app.Model.Album;
import com.melodix.app.R;

import java.util.List;

public class ManageAlbumAdapter extends RecyclerView.Adapter<ManageAlbumAdapter.AlbumViewHolder> {

    private final Context context;
    private final List<Album> albumList;
    private final OnAlbumOptionClickListener listener;

    // Upgraded interface: now it has 2 clear responsibilities
    public interface OnAlbumOptionClickListener {
        void onAlbumClick(Album album);   // Tap the album card to view tracks
        void onOptionClick(Album album);  // Tap the 3-dot button / trash button
    }

    public ManageAlbumAdapter(Context context, List<Album> albumList, OnAlbumOptionClickListener listener) {
        this.context = context;
        this.albumList = albumList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albumList.get(position);

        holder.tvTitle.setText(album.title);
        holder.tvYear.setText("Release Year: " + album.year);

        if (album.coverRes != null && !album.coverRes.isEmpty()) {
            Glide.with(context).load(album.coverRes).into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.ic_launcher_background);
        }

        String status = album.status != null ? album.status.toLowerCase() : "pending";
        switch (status) {
            case "approved":
                holder.tvStatus.setText("Approved");
                holder.tvStatus.setTextColor(Color.parseColor("#1DB954"));
                break;
            case "rejected":
                holder.tvStatus.setText("Rejected");
                holder.tvStatus.setTextColor(Color.parseColor("#FF453A"));
                break;
            default:
                holder.tvStatus.setText("Pending Review");
                holder.tvStatus.setTextColor(Color.parseColor("#FF9F0A"));
                break;
        }

        // Handle click on the whole album card
        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(album));

        // Handle click on the 3-dot / trash button
        holder.btnOptions.setOnClickListener(v -> listener.onOptionClick(album));
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover, btnOptions;
        TextView tvTitle, tvYear, tvStatus;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_album_cover);
            tvTitle = itemView.findViewById(R.id.tv_album_title);
            tvYear = itemView.findViewById(R.id.tv_album_year);
            tvStatus = itemView.findViewById(R.id.tv_album_status);
            btnOptions = itemView.findViewById(R.id.btn_album_options);
        }
    }
}