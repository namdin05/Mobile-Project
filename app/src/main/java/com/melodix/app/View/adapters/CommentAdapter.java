package com.melodix.app.View.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.melodix.app.Model.Comment;
import com.melodix.app.R;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private final Context context;
    private final List<Comment> comments;

    public CommentAdapter(Context context, List<Comment> comments) {
        this.context = context;
        this.comments = comments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);

        // Tên người dùng
        String displayName = comment.getDisplayName();
        holder.tvUser.setText(displayName != null ? displayName : "Người dùng");

        // Nội dung bình luận
        holder.tvText.setText(comment.content);

        // Thời gian
        if (comment.createdAt != null && !comment.createdAt.isEmpty()) {
            // Hiển thị đơn giản (có thể cải tiến sau bằng TimeUtils)
            String time = comment.createdAt.substring(0, 10); // YYYY-MM-DD
            holder.tvTime.setText(time);
        } else {
            holder.tvTime.setText("Vừa xong");
        }

        // Avatar
        String avatarUrl = comment.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_default_avatar);
        }

        // Mood emoji (tạm thời để mặc định, bạn có thể thêm logic random hoặc theo nội dung sau)
        holder.tvMood.setText("❤️");   // Có thể thay đổi thành 🔥, 👍, 🎵... tùy theo cảm xúc
    }

    @Override
    public int getItemCount() {
        return comments != null ? comments.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUser;
        TextView tvMood;
        TextView tvTime;
        TextView tvText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            tvUser = itemView.findViewById(R.id.tv_user);
            tvMood = itemView.findViewById(R.id.tv_mood);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvText = itemView.findViewById(R.id.tv_text);
        }
    }
}
