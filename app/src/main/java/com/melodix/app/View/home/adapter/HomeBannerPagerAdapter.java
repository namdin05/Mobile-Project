package com.melodix.app.View.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.melodix.app.Model.HomeBanner;
import com.melodix.app.R;

import java.util.ArrayList;
import java.util.List;

public class HomeBannerPagerAdapter extends RecyclerView.Adapter<HomeBannerPagerAdapter.BannerViewHolder> {

    public interface OnBannerClickListener {
        void onBannerClick(HomeBanner banner);
    }

    private final List<HomeBanner> banners = new ArrayList<>();
    private final OnBannerClickListener listener;

    public HomeBannerPagerAdapter(OnBannerClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<HomeBanner> newItems) {
        banners.clear();

        if (newItems != null) {
            banners.addAll(newItems);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        HomeBanner banner = banners.get(position);

        holder.tvBannerTitle.setText(banner.getDisplayTitle());
        holder.tvBannerSubtitle.setText(banner.getDisplaySubtitle());

        try {
            Glide.with(holder.itemView.getContext())
                    .load(banner.getImageUrl())
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .centerCrop()
                    .into(holder.ivBannerImage);
        } catch (Exception ignored) {
        }

        holder.cardBannerRoot.setOnClickListener(v -> {
            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (listener != null) {
                listener.onBannerClick(banners.get(adapterPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return banners.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardBannerRoot;
        ImageView ivBannerImage;
        TextView tvBannerTitle;
        TextView tvBannerSubtitle;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBannerRoot = itemView.findViewById(R.id.cardBannerRoot);
            ivBannerImage = itemView.findViewById(R.id.ivBannerImage);
            tvBannerTitle = itemView.findViewById(R.id.tvBannerTitle);
            tvBannerSubtitle = itemView.findViewById(R.id.tvBannerSubtitle);
        }
    }
}