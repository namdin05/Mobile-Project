package com.melodix.app.View.search.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.melodix.app.R;

public class CategoryAdapter extends ListAdapter<String, CategoryAdapter.CategoryViewHolder> {

    private static final DiffUtil.ItemCallback<String> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<String>() {
                @Override
                public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private static final int[][] GRADIENT_PALETTES = new int[][]{
            {Color.parseColor("#1DB983"), Color.parseColor("#12805D")},
            {Color.parseColor("#7C3AED"), Color.parseColor("#4C1D95")},
            {Color.parseColor("#06B6D4"), Color.parseColor("#0F766E")},
            {Color.parseColor("#F97316"), Color.parseColor("#C2410C")},
            {Color.parseColor("#EC4899"), Color.parseColor("#9D174D")},
            {Color.parseColor("#64748B"), Color.parseColor("#334155")}
    };

    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    public CategoryAdapter(OnCategoryClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_grid, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        final String categoryName = getItem(position);

        holder.tvCategoryName.setText(categoryName);
        holder.viewCategoryGradient.setBackground(createGradientDrawable(position));

        holder.cardCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onCategoryClick(categoryName);
                }
            }
        });
    }

    private GradientDrawable createGradientDrawable(int position) {
        int[] colors = GRADIENT_PALETTES[position % GRADIENT_PALETTES.length];
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardCategory;
        private final View viewCategoryGradient;
        private final TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCategory = itemView.findViewById(R.id.cardCategory);
            viewCategoryGradient = itemView.findViewById(R.id.viewCategoryGradient);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}