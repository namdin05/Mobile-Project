package com.melodix.app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import java.util.List;

public class SongRequestAdapter extends RecyclerView.Adapter<SongRequestAdapter.ViewHolder> {

    private List<Song> requestList;
    private OnItemActionListener actionListener;

    public interface OnItemActionListener {
        void onApproveClick(Song request, int position);
        void onRejectClick(Song request, int position);
        void onPlayClick(Song request); // SỰ KIỆN MỚI: NGHE THỬ
    }

    public SongRequestAdapter(List<Song> requestList, OnItemActionListener actionListener) {
        this.requestList = requestList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song request = requestList.get(position);

        holder.tvSongTitle.setText(request.getTitle());
        holder.tvArtistName.setText(request.getArtistName()); // Bây giờ nó sẽ lấy được tên nhờ hàm Join

        // Lắng nghe sự kiện click
        holder.btnApprove.setOnClickListener(v -> actionListener.onApproveClick(request, position));
        holder.btnReject.setOnClickListener(v -> actionListener.onRejectClick(request, position));

        // Bấm vào nguyên cái khối (Card) để phát nhạc
        holder.itemView.setOnClickListener(v -> actionListener.onPlayClick(request));
    }

    @Override
    public int getItemCount() {
        return requestList != null ? requestList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgCover;
        TextView tvSongTitle, tvArtistName;
        MaterialButton btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
