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
import com.melodix.app.Model.Artist;
import com.melodix.app.R;
import com.melodix.app.View.ArtistDetailActivity;
import java.util.ArrayList;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {
    private Context context;
    private ArrayList<Artist> artists;

    public ArtistAdapter(Context context, ArrayList<Artist> artists) {
        this.context = context;
        this.artists = artists;
    }

    public void update(ArrayList<Artist> newArtists) {
        this.artists.clear();
        this.artists.addAll(newArtists);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artists.get(position);
        holder.tvName.setText(artist.name);

        // Tự động bo tròn ảnh bằng circleCrop()
        Glide.with(context)
                .load(artist.avatarRes)
                .error(android.R.color.darker_gray)
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(holder.imgAvatar);

        // Bấm vào thì mở TRANG MỚI của Nghệ sĩ đó (để xem Related Artist)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ArtistDetailActivity.class);
            intent.putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, artist.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_artist_avatar);
            tvName = itemView.findViewById(R.id.tv_artist_name);
        }
    }
}