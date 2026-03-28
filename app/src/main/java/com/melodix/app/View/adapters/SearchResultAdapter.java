package com.melodix.app.View.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.R;
import com.melodix.app.Utils.ResourceUtils;
import java.util.ArrayList;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ResultHolder> {
    public interface OnResultClickListener { void onResultClick(SearchResultItem item); }
    private final Context context;
    private final ArrayList<SearchResultItem> items;
    private final OnResultClickListener listener;

    public SearchResultAdapter(Context context, ArrayList<SearchResultItem> items, OnResultClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void update(ArrayList<SearchResultItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ResultHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ResultHolder holder, int position) {
        SearchResultItem item = items.get(position);
        holder.cover.setImageResource(ResourceUtils.anyDrawable(context, item.coverRes));
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onResultClick(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ResultHolder extends RecyclerView.ViewHolder {
        ImageView cover; TextView title; TextView subtitle;
        ResultHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.img_cover);
            title = itemView.findViewById(R.id.tv_title);
            subtitle = itemView.findViewById(R.id.tv_subtitle);
        }
    }
}
