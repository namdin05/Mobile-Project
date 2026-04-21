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

import com.bumptech.glide.Glide;
import com.melodix.app.Model.DownloadedSong;
import com.melodix.app.R;
import com.melodix.app.Utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

//Hiển thị danh sach bài hát đã tải
public class DownloadedSongAdapter extends RecyclerView.Adapter<DownloadedSongAdapter.ViewHolder> {

    private final Context context;
    private List<DownloadedSong> downloadedSongs = new ArrayList<>();

    // Listener khi click vào bài hát đã tải
    public interface OnDownloadedSongClickListener {
        void onSongClick(DownloadedSong song);
        void onMoreClick(DownloadedSong song, int position);
    }

    private final OnDownloadedSongClickListener listener;

    public DownloadedSongAdapter(Context context) {
        this.context = context;
        this.listener = null;
    }

    public DownloadedSongAdapter(Context context, OnDownloadedSongClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    //Cập nhật danh sách bài hát đã tải
    public void updateList(List<DownloadedSong> newList) {
        this.downloadedSongs.clear();
        if (newList != null) {
            this.downloadedSongs.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadedSong song = downloadedSongs.get(position);

        // Hiển thị tiêu đề và nghệ sĩ
        holder.tvTitle.setText(song.title != null ? song.title : "Không có tiêu đề");
        holder.tvSubtitle.setText(song.artistName != null ? song.artistName : "Unknown Artist");

        // Hiển thị thời lượng
        if (song.durationSeconds > 0) {
            holder.tvMeta.setText(TimeUtils.formatDuration(song.durationSeconds));
        } else {
            holder.tvMeta.setText("--:--");
        }

        // Load ảnh bìa (ưu tiên localCoverPath, nếu không có thì dùng coverUrl gốc)
        if (song.localCoverPath != null && !song.localCoverPath.isEmpty()) {
            Glide.with(context)
                    .load(song.localCoverPath)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .into(holder.imgCover);
        } else if (song.coverUrl != null && !song.coverUrl.isEmpty()) {
            Glide.with(context)
                    .load(song.coverUrl)
                    .placeholder(R.drawable.ic_music_placeholder)
                    .error(R.drawable.ic_music_placeholder)
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(R.drawable.ic_music_placeholder);
        }

        // Click vào item → phát nhạc offline
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(song);
            }
        });

        // Click nút More
        holder.btnMore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreClick(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return downloadedSongs.size();
    }

   //ViewHolder
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvMeta;
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