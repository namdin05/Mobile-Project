package com.melodix.app.View.home;

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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.melodix.app.Model.Song;
import com.melodix.app.R;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongHolder> {
    Context context;

    public interface OnSongClickListener {
        void onSongClick(Song song);

        void onMenuClick(Song song, int position, String action);
    }

    private OnSongClickListener listener;
    private List<Song> songs;

    public SongAdapter(Context context, List<Song> songs, OnSongClickListener listener) {
        this.context = context;
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SongHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SongHolder holder, int position) {
        Song song = songs.get(position);
        Glide.with(context).load(song.getCover_url()).into(holder.cover);
        holder.title.setText(song.getTitle());

        // THÊM DÒNG NÀY: Hiển thị tên nghệ sĩ
        holder.artist.setText(song.getArtistName() != null ? song.getArtistName() : "Unknown Artist");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSongClick(song);
        });
        holder.more.setOnClickListener(v -> {
            if(listener != null) showMenu(song, position);
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class SongHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title;
        TextView artist;
        TextView meta;
        ImageButton more;

        SongHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.img_cover);
            title = itemView.findViewById(R.id.tv_title);
            artist = itemView.findViewById(R.id.tv_subtitle);
            meta = itemView.findViewById(R.id.tv_meta);
            more = itemView.findViewById(R.id.btn_more);
        }
    }

    private void showMenu(Song song, int position){
        BottomSheetDialog bottomSheet = new BottomSheetDialog(context, R.style.BottomSheetTheme);
        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.dialog_song_menu, null);
        bottomSheet.setContentView(bottomSheetView);

        bottomSheetView.findViewById(R.id.menu_play).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "play");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_like).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "like");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_add_playlist).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "playlist");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_comments).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "comment");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_share).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "share");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_download).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "download");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_remove_playlist).setOnClickListener(v -> {
            if (listener != null) listener.onMenuClick(song, position, "remove");
            bottomSheet.dismiss();
        });

        // 4. Hiển thị menu lên màn hình
        bottomSheet.show();
    }
}
