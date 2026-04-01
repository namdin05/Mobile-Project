package com.melodix.app.View.adapters;

import android.content.Context;
import android.util.Log;
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

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreHolder> {
    public interface OnGenreClickListener {
        void onGenreClick(Genre genre);
    }
    private final Context context;
    private final List<Genre> genres;
    private OnGenreClickListener listener;


    public GenreAdapter(Context context, List<Genre> genres, OnGenreClickListener listener) {
        this.context = context;
        this.genres = genres;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GenreHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GenreHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_genre, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GenreHolder holder, int position) {
        Genre genre = genres.get(position);
        Glide.with(context).load(genre.getCover_url()).into(holder.image);
        Log.d("GEN_IMG", genre.getCover_url());
        holder.name.setText(genre.getName());
        holder.itemView.setOnClickListener(v -> {
            if(listener != null) listener.onGenreClick(genre);
        });
    }

    @Override
    public int getItemCount() { return genres.size(); }

    static class GenreHolder extends RecyclerView.ViewHolder {
        ImageView image; TextView name;
        GenreHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.img_genre);
            name = itemView.findViewById(R.id.tv_name);
        }
    }
}
