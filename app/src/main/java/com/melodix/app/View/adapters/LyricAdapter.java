package com.melodix.app.View.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Đã thêm thư viện này để lấy màu chuẩn
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.LyricLine;
import com.melodix.app.R;

import java.util.ArrayList;
import java.util.List;

public class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.ViewHolder> {
    private ArrayList<LyricLine> list;
    private int currentHighlightIndex = -1;

    // 1. THÊM BỘ ĐÀM LIÊN LẠC
    public interface OnLyricClickListener {
        void onLyricClick(long timeMs);
    }
    private OnLyricClickListener listener;

    // 2. SỬA LẠI HÀM KHỞI TẠO ĐỂ NHẬN BỘ ĐÀM
    public LyricAdapter(ArrayList<LyricLine> list, OnLyricClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void setHighlightIndex(int index) {
        if (this.currentHighlightIndex == index) return;

        int oldIndex = this.currentHighlightIndex;
        this.currentHighlightIndex = index;

        // Bắn tín hiệu cập nhật
        if (oldIndex != -1) notifyItemChanged(oldIndex, false);
        if (this.currentHighlightIndex != -1) notifyItemChanged(this.currentHighlightIndex, true);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lyric, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        holder.tvText.setText(list.get(position).text);

        // ==========================================
        // ĐÃ SỬA: TỰ ĐỘNG LẤY MÀU SÁNG/TỐI TỪ THEME
        // ==========================================
        int activeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.mdx_primary); // Xanh Spotify cho dòng đang hát
        int inactiveColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.mdx_text);  // Trắng/Đen cho dòng chưa hát

        // BƯỚC 1: Rút phích cắm mọi animation đang chạy ngầm trên view này
        holder.tvText.animate().cancel();

        // 3. BẮT SỰ KIỆN CLICK VÀO DÒNG CHỮ
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLyricClick(list.get(position).timeMs);
            }
        });

        boolean isHighlight = (position == currentHighlightIndex);
        if (payloads.isEmpty()) {
            // BƯỚC 2: NẾU LÀ VUỐT MÀN HÌNH -> Gắn cứng trạng thái tĩnh
            holder.tvText.setTextColor(isHighlight ? activeColor : inactiveColor);
            holder.tvText.setAlpha(isHighlight ? 1.0f : 0.4f);
            holder.tvText.setScaleX(isHighlight ? 1.1f : 1.0f);
            holder.tvText.setScaleY(isHighlight ? 1.1f : 1.0f);
        } else {
            // BƯỚC 3: NẾU LÀ TỰ ĐỘNG CHUYỂN CÂU HÁT -> Chạy animation mượt mà
            if (isHighlight) {
                holder.tvText.setTextColor(activeColor);
                holder.tvText.animate().alpha(1.0f).scaleX(1.1f).scaleY(1.1f).setDuration(300).start();
            } else {
                holder.tvText.setTextColor(inactiveColor);
                holder.tvText.animate().alpha(0.4f).scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bỏ trống vì đã xài hàm payloads ở trên
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tv_lyric);
        }
    }
}