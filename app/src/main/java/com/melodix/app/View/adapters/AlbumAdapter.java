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

        // 1. Gán Tên Album (Dòng 1)
        if (album.title != null) {
            holder.tvTitle.setText(album.title);
        }

        // 2. Gán Tên Ca sĩ & Năm phát hành (Dòng 2)
        // Tạo một chuỗi rỗng để chứa dữ liệu
        StringBuilder subtitle = new StringBuilder();

        if (album.artistName != null && !album.artistName.isEmpty()) {
            subtitle.append(album.artistName);
        }

        // Kiểm tra xem có năm phát hành không (thường > 0)
        if (album.year > 0) {
            // Nếu đã có tên ca sĩ trước đó, thêm dấu chấm tròn " • " ngăn cách
            if (subtitle.length() > 0) {
                subtitle.append(" • ");
            }
            subtitle.append(album.year);
        }

        // Đẩy chuỗi vừa ghép lên giao diện
        if (subtitle.length() > 0) {
            holder.tvArtistName.setText(subtitle.toString());
            holder.tvArtistName.setVisibility(View.VISIBLE);
        } else {
            holder.tvArtistName.setVisibility(View.GONE); // Ẩn nếu không có cả tên lẫn năm
        }

        // 3. Load ảnh bằng Glide (Giữ nguyên như cũ)
        if (album.coverRes != null && !album.coverRes.isEmpty()) {
            Glide.with(context)
                    .load(album.coverRes)
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.darker_gray)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(holder.imgCover);
        } else {
            holder.imgCover.setImageResource(android.R.color.darker_gray);
        }

        // 4. Sự kiện click (Giữ nguyên như cũ)
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
        TextView tvArtistName; // Bổ sung biến cho tên ca sĩ

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.img_album_cover);
            tvTitle = itemView.findViewById(R.id.tv_album_title);

            // Bổ sung ánh xạ ID cho tên ca sĩ.
            // (LƯU Ý: Mở file R.layout.item_album ra kiểm tra xem ID của TextView ca sĩ tên là gì, nếu không phải tv_album_artist thì bạn đổi lại cho đúng nhé)
            tvArtistName = itemView.findViewById(R.id.tv_album_artist);
        }
    }
}