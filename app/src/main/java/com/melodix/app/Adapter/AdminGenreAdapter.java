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

    public AdminGenreAdapter(Context context, List<Genre> genreList) {
        this.context = context;
        this.genreList = genreList;
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_genre, parent, false);
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
            imgGenreCover = itemView.findViewById(R.id.imgGenreCover);
            tvGenreName = itemView.findViewById(R.id.tvGenreName);
        }
    }
}