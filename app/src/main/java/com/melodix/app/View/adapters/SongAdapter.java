package com.melodix.app.View.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.melodix.app.Model.Song;
import com.melodix.app.Repository.DownloadRepository;
import com.melodix.app.R;
import com.melodix.app.Utils.TimeUtils;
import com.melodix.app.View.dialogs.PlaylistSelectionDialog;
import com.melodix.app.View.music.CommentsBottomSheet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongHolder> {
    public interface OnSongActionListener {
        void onSongClick(Song song, int position);
        void onMenuClick(Song song, int position, String actionId);
    }

    private final Context context;

    private boolean isAnalyticsMode = false; // Mặc định là TẮT

    // Thêm hàm này để BẬT công tắc từ bên ngoài vào
    public void setAnalyticsMode(boolean isAnalyticsMode) {
        this.isAnalyticsMode = isAnalyticsMode;
    }
    private final List<Song> songs;
    private final OnSongActionListener listener;

    public SongAdapter(Context context, List<Song> songs, OnSongActionListener listener) {
        this.context = context;
        this.songs = songs;
        this.listener = listener;
    }

    public List<Song> getSongs() { return songs; }

    public void swapItems(int from, int to) {
        if (from < 0 || to < 0 || from >= songs.size() || to >= songs.size()) return;
        Collections.swap(songs, from, to);
        notifyItemMoved(from, to);
    }

    @NonNull
    @Override
    public SongHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SongHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SongHolder holder, int position) {
        Song song = songs.get(position);

        com.bumptech.glide.Glide.with(context)
                .load(song.getCoverUrl())
                .into(holder.cover);

        holder.title.setText(song.getTitle());
        String displayArtist = (song.getArtistName() != null && !song.getArtistName().trim().isEmpty() && !song.getArtistName().equalsIgnoreCase("null"))
                ? song.getArtistName()
                : "Unknown Artist";

        if (song.getGenre() != null && !song.getGenre().trim().isEmpty() && !song.getGenre().equalsIgnoreCase("null")) {
            holder.subtitle.setText(song.getArtistName() + " • " + song.getGenre());
        } else {
            holder.subtitle.setText(song.getArtistName()); // Nếu không có thể loại, chỉ hiện tên nghệ sĩ
        }

        holder.meta.setText(TimeUtils.formatDuration(song.getDurationSeconds()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(song, position);
        });

        holder.more.setOnClickListener(v -> showMenu(v, song, position));

        if (isAnalyticsMode) {
            // NẾU BẬT CÔNG TẮC: Hiển thị 🎧 Lượt nghe và ❤️ Lượt thích
            java.text.NumberFormat format = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
            String stats = "🎧 " + format.format(song.getPlays()) + "   •   ❤️ " + format.format(song.getLikes());

            holder.subtitle.setText(stats); // (Nhớ đổi tvArtistName thành tên biến TextView của bạn nếu khác nhé)
            holder.subtitle.setTextColor(android.graphics.Color.parseColor("#1DB954")); // Đổi màu xanh lá cho nổi bật số liệu
        } else {
            // NẾU TẮT CÔNG TẮC: Trả về hiển thị tên nghệ sĩ như bình thường
            holder.subtitle.setText(song.getArtistName());
            holder.subtitle.setTextColor(android.graphics.Color.parseColor("#AAAAAA")); // Trả về màu xám mặc định
        }
    }

    private void showMenu(View anchor, Song song, int position) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetTheme);
        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.dialog_song_menu, null);
        bottomSheet.setContentView(bottomSheetView);

        // Xử lý nền trong suốt để viền bo góc của bg_card hiện ra
        View parent = (View) bottomSheetView.getParent();
        if (parent != null) {
            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        // Gắn sự kiện click cho từng dòng menu
        bottomSheetView.findViewById(R.id.menu_play).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "play");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_like).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "like");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_add_playlist).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showPlaylistSelectionDialog(song);
        });

        bottomSheetView.findViewById(R.id.menu_comments).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "comment");
            bottomSheet.dismiss();
            showCommentsBottomSheet(song);
        });

        bottomSheetView.findViewById(R.id.menu_share).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "share");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_download).setOnClickListener(v -> {
            try {
                if (song == null || song.getId() == null) {
                    Toast.makeText(context, "Không thể tải bài hát này", Toast.LENGTH_SHORT).show();
                    return;
                }

                DownloadRepository repo = new DownloadRepository(context);
                repo.enqueueDownload(song);

            } catch (Exception e) {
                Log.e("DOWNLOAD_CRASH", "Lỗi khi gọi download", e);
                Toast.makeText(context, "Lỗi tải xuống: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            bottomSheet.dismiss();
        });

        TextView menuRemove = bottomSheetView.findViewById(R.id.menu_remove_playlist);
        if (listener != null) {
            menuRemove.setOnClickListener(v -> {
                if (listener != null) listener.onMenuClick(song, position, "remove");
                bottomSheet.dismiss();
            });
        } else {
            menuRemove.setVisibility(View.GONE);
        }


        bottomSheet.show();
    }

    private void showCommentsBottomSheet(Song song) {
        if (context instanceof FragmentActivity) {
            CommentsBottomSheet commentsBottomSheet = CommentsBottomSheet.newInstance(song.getId());
            commentsBottomSheet.show(((FragmentActivity) context).getSupportFragmentManager(), "comments_bottom_sheet");
        }
    }
    private void showPlaylistSelectionDialog(Song song) {
        if (context instanceof androidx.fragment.app.FragmentActivity) {
            PlaylistSelectionDialog dialog = PlaylistSelectionDialog.newInstance(song.getId());
            dialog.setOnPlaylistActionListener(() -> {
            });
            dialog.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "playlist_selection");
        }
    }

    @Override
    public int getItemCount() {
        if(songs != null) return songs.size();

        return 0;
    }

    static class SongHolder extends RecyclerView.ViewHolder {
        ImageView cover; TextView title; TextView subtitle; TextView meta; ImageButton more;
        SongHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.img_cover);
            title = itemView.findViewById(R.id.tv_title);
            subtitle = itemView.findViewById(R.id.tv_subtitle);
            meta = itemView.findViewById(R.id.tv_meta);
            more = itemView.findViewById(R.id.btn_more);
        }
    }

    public void update(ArrayList<Song> newSongs) {
        if (newSongs != null)
        {
            this.songs.clear();
            this.songs.addAll(newSongs);
            notifyDataSetChanged();
        }

    }
}