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
    private List<String> selectedSongIds; // Nhận danh sách các ID đã chọn từ Activity

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

        // Tránh lỗi null pointer nếu bài hát chưa có tên nghệ sĩ
        holder.tvArtist.setText(song.getArtistName() != null ? song.getArtistName() : "Nghệ sĩ");

        Glide.with(context)
                .load(song.getCoverUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imgCover);

        // 1. ẨN NÚT 3 CHẤM (Vì đang ở chế độ chọn bài, không cần xài menu)
        if (holder.btnMore != null) {
            holder.btnMore.setVisibility(View.GONE);
        }

        // 2. PHÉP THUẬT ĐỔI MÀU NỀN
        if (selectedSongIds.contains(song.getId())) {
            // Đã chọn -> Đổi nền thành Xanh lá cây nhạt (Mã màu Spotify)
            holder.itemView.setBackgroundColor(Color.parseColor("#331DB954"));
        } else {
            // Chưa chọn -> Nền trong suốt trở lại
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        // 3. BẮT SỰ KIỆN CLICK: CHỌN / BỎ CHỌN
        holder.itemView.setOnClickListener(v -> {
            if (selectedSongIds.contains(song.getId())) {
                selectedSongIds.remove(song.getId()); // Đang chọn mà bấm nữa -> Bỏ chọn
            } else {
                selectedSongIds.add(song.getId()); // Chưa chọn -> Thêm vào danh sách
            }

            // Lệnh này cực kỳ quan trọng: Báo cho Adapter biết "Ê, dòng này bị đổi rồi, vẽ lại màu nền đi!"
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
            // 👉 SỬA LẠI CÁC ID DƯỚI ĐÂY CHO KHỚP VỚI FILE item_song.xml CỦA BẠN NHÉ
            imgCover = itemView.findViewById(R.id.img_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_subtitle);
            btnMore = itemView.findViewById(R.id.btn_more);   // Nút menu 3 chấm
        }
    }
}