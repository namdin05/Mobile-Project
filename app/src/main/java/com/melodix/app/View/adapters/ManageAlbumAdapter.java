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
    private final OnAlbumClickListener listener;

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album);
    }

    public ManageAlbumAdapter(Context context, List<Album> albumList, OnAlbumClickListener listener) {
        this.context = context;
        this.albumList = albumList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Thay item_manage_album bằng tên file XML của bạn nếu khác
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albumList.get(position);

        holder.tvTitle.setText(album.title);
        holder.tvYear.setText("Năm phát hành: " + album.year);

        if (album.coverRes != null && !album.coverRes.isEmpty()) {
            Glide.with(context).load(album.coverRes).into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.ic_launcher_background); // Ảnh mặc định
        }

        // Xử lý hiển thị trạng thái
        String status = album.status != null ? album.status.toLowerCase() : "pending";
        switch (status) {
            case "approved":
                holder.tvStatus.setText("Đã duyệt");
                holder.tvStatus.setTextColor(Color.parseColor("#1DB954")); // Xanh Spotify
                break;
            case "rejected":
                holder.tvStatus.setText("Bị từ chối");
                holder.tvStatus.setTextColor(Color.parseColor("#FF453A")); // Đỏ
                break;
            default: // pending
                holder.tvStatus.setText("Đang chờ duyệt");
                holder.tvStatus.setTextColor(Color.parseColor("#FF9F0A")); // Cam
                break;
        }

        holder.itemView.setOnClickListener(v -> listener.onAlbumClick(album));
    }

    @Override
    public int getItemCount() {
        return albumList.size();
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle, tvYear, tvStatus;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            // Nhớ khớp ID với file item_manage_album.xml của bạn nhé
            imgCover = itemView.findViewById(R.id.img_album_cover);
            tvTitle = itemView.findViewById(R.id.tv_album_title);
            tvYear = itemView.findViewById(R.id.tv_album_year);
            tvStatus = itemView.findViewById(R.id.tv_album_status);
        }
    }
}