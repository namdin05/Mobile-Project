package com.melodix.app.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.melodix.app.Model.Genre;
import com.melodix.app.R;

import java.util.List;

public class AdminGenreAdapter extends RecyclerView.Adapter<AdminGenreAdapter.GenreViewHolder> {

    private Context context;
    private List<Genre> genreList;
    private OnGenreClickListener listener;

    public interface OnGenreClickListener {
        void onGenreClick(Genre genre);
    }

    public AdminGenreAdapter(Context context, List<Genre> genreList, OnGenreClickListener listener) {
        this.context = context;
        this.genreList = genreList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_genre, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        Genre genre = genreList.get(position);

        holder.tvGenreName.setText(genre.getName());

        // Dùng Glide để load ảnh
        if (genre.getCoverUrl() != null && !genre.getCoverUrl().isEmpty()) {
            Glide.with(context)
                    .load(genre.getCoverUrl())
                    // Thay ic_music_note bằng ảnh placeholder của bạn
                    .placeholder(R.drawable.ic_person)
                    .into(holder.imgGenreCover);
        }

        // 4. Bắt sự kiện Click vào toàn bộ Item và báo về cho Fragment
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGenreClick(genre);
            }
        });

        if (!genre.isVisible()) {
            holder.itemView.setAlpha(0.4f); // Làm mờ 60% nếu bị xóa mềm
        } else {
            holder.itemView.setAlpha(1.0f); // Sáng rõ nếu bình thường
        }
    }

    @Override
    public int getItemCount() {
        return genreList != null ? genreList.size() : 0;
    }

    public static class GenreViewHolder extends RecyclerView.ViewHolder {
        ImageView imgGenreCover;
        TextView tvGenreName;

        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            imgGenreCover = itemView.findViewById(R.id.img_genre);
            tvGenreName = itemView.findViewById(R.id.tv_name);
        }
    }
}