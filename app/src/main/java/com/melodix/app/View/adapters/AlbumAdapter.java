package com.melodix.app.View.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.melodix.app.Model.Album;
import com.melodix.app.R;
import com.melodix.app.View.AlbumDetailActivity;
import java.util.ArrayList;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
    private Context context;
    private ArrayList<Album> albums;

    public AlbumAdapter(Context context, ArrayList<Album> albums) {
        this.context = context;
        this.albums = albums;
    }

    public void update(ArrayList<Album> newAlbums) {
        this.albums.clear();
        this.albums.addAll(newAlbums);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.tvTitle.setText(album.title);

        Glide.with(context)
                .load(album.coverRes)
                .error(android.R.color.darker_gray)
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(holder.imgCover);

        // Bấm vào thì mở trang Chi tiết Album đó
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AlbumDetailActivity.class);
            intent.putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, album.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvTitle;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_album_cover);
            tvTitle = itemView.findViewById(R.id.tv_album_title);
        }
    }
}