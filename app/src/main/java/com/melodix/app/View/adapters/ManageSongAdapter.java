package com.melodix.app.View.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

public class ManageSongAdapter extends RecyclerView.Adapter<ManageSongAdapter.SongViewHolder> {

    private final Context context;
    private final List<Song> songList;
    public interface OnSongOptionClickListener{
        void onOptionClick(Song song);
    }
    private final OnSongOptionClickListener optionClickListener;
    public ManageSongAdapter(Context context, List<Song> songList, OnSongOptionClickListener listener) {
        this.context = context;
        this.songList = songList;
        this.optionClickListener = listener;
    }



    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_manage_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);

        holder.tvTitle.setText(song.getTitle());

        // Tính toán hiển thị phút:giây
        int dur = song.getDurationSeconds();
        String timeString = String.format("%02d:%02d", dur / 60, dur % 60);

        // Cập nhật text phụ
        holder.tvStats.setText("🎧 " + song.getPlays() + " lượt nghe • " + timeString);
        Glide.with(context).load(song.getCoverUrl()).into(holder.imgCover);


        // Tạo khung bo góc tròn cho cái Nhãn trạng thái
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setCornerRadius(40);

        // Lấy trạng thái (đảm bảo model Song của bạn có hàm getStatus() nhé)
        String status = song.getStatus() != null ? song.getStatus().toLowerCase() : "pending";

        switch (status) {
            case "approved":
                holder.tvStatus.setText("✓ Đã phát hành");
                badgeBg.setColor(Color.parseColor("#1DB954")); // Xanh lá Spotify
                break;
            case "rejected":
                holder.tvStatus.setText("✖ Từ chối");
                badgeBg.setColor(Color.parseColor("#FF453A")); // Đỏ
                break;
            default: // pending
                holder.tvStatus.setText("⏳ Đang chờ duyệt");
                badgeBg.setColor(Color.parseColor("#FF9F0A")); // Vàng cam
                break;
        }

        holder.tvStatus.setBackground(badgeBg);

        holder.btnOptions.setOnClickListener(v -> {
            if (optionClickListener != null) {
                optionClickListener.onOptionClick(song);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover, btnOptions;
        TextView tvTitle, tvStats, tvStatus; // <-- Thêm tvStats
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_song_cover);
            tvTitle = itemView.findViewById(R.id.tv_song_title);
            tvStats = itemView.findViewById(R.id.tv_song_stats); // <-- Ánh xạ nó
            tvStatus = itemView.findViewById(R.id.tv_song_status);
            btnOptions = itemView.findViewById(R.id.btn_song_options); // <-- Ánh xạ
        }
    }
}