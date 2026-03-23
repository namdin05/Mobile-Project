package com.melodix.app.View.lyrics.adapter;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.LyricLine;
import com.melodix.app.R;

import java.util.ArrayList;
import java.util.List;

public class LyricLineAdapter extends RecyclerView.Adapter<LyricLineAdapter.LyricViewHolder> {

    private final List<LyricLine> lines = new ArrayList<>();
    private boolean syncedMode = false;
    private int activeIndex = -1;

    public void submitLyrics(List<LyricLine> newLines, boolean syncedMode) {
        lines.clear();

        if (newLines != null) {
            lines.addAll(newLines);
        }

        this.syncedMode = syncedMode;
        this.activeIndex = -1;
        notifyDataSetChanged();
    }

    public void setActiveIndex(int newIndex) {
        if (!syncedMode) {
            newIndex = -1;
        }

        if (this.activeIndex == newIndex) {
            return;
        }

        int oldIndex = this.activeIndex;
        this.activeIndex = newIndex;

        if (oldIndex >= 0 && oldIndex < lines.size()) {
            notifyItemChanged(oldIndex);
        }

        if (newIndex >= 0 && newIndex < lines.size()) {
            notifyItemChanged(newIndex);
        }
    }

    @NonNull
    @Override
    public LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lyric_line, parent, false);
        return new LyricViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LyricViewHolder holder, int position) {
        LyricLine line = lines.get(position);
        String text = line == null ? "" : line.getDisplayText();

        holder.tvLyricLine.setText(TextUtils.isEmpty(text) ? " " : text);

        if (syncedMode) {
            boolean isActive = position == activeIndex;

            holder.tvLyricLine.setTextColor(ContextCompat.getColor(
                    holder.itemView.getContext(),
                    isActive ? R.color.spotify_text_primary : R.color.spotify_text_secondary
            ));
            holder.tvLyricLine.setTypeface(isActive ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            holder.tvLyricLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, isActive ? 28f : 20f);
            holder.tvLyricLine.setBackgroundResource(isActive ? R.drawable.bg_lyric_active_line : android.R.color.transparent);
        } else {
            holder.tvLyricLine.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.spotify_text_primary));
            holder.tvLyricLine.setTypeface(Typeface.DEFAULT);
            holder.tvLyricLine.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            holder.tvLyricLine.setBackgroundResource(android.R.color.transparent);
        }
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    static class LyricViewHolder extends RecyclerView.ViewHolder {

        TextView tvLyricLine;

        public LyricViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLyricLine = itemView.findViewById(R.id.tvLyricLine);
        }
    }
}