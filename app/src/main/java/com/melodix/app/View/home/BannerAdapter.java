package com.melodix.app.View.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.melodix.app.Model.Banner;
import com.melodix.app.R;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    public interface OnBannerClickListener { void onBannerClick(Banner item); }
    private final Context context;
    private final List<Banner> items;
    private final OnBannerClickListener listener;

    public BannerAdapter(Context context, List<Banner> items, OnBannerClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BannerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner item = items.get(position);
//        holder.title.setText(item.getTitle()+"");
//        holder.badge.setText("HOT");
        Glide.with(context).load(item.getCover_url()).into(holder.image);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBannerClick(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView image; TextView title; TextView badge;
        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
//            title = itemView.findViewById(R.id.tv_title);
            image = itemView.findViewById(R.id.img_banner);
//            badge = itemView.findViewById(R.id.tv_badge);
        }
    }
}
