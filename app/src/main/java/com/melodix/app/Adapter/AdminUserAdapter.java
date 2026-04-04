package com.melodix.app.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.melodix.app.Model.Profile;
import com.melodix.app.R;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private List<Profile> userList;

    public AdminUserAdapter(List<Profile> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Profile user = userList.get(position);
        holder.tvUserName.setText(user.getDisplayName());

        // Viết hoa chữ cái đầu của Role (VD: "artist" -> "Artist")
        String roleStr = user.getRole().substring(0, 1).toUpperCase() + user.getRole().substring(1);
        holder.tvUserRole.setText(roleStr);

        holder.btnBan.setOnClickListener(v -> {
            // TODO: Bắt sự kiện Ban User ở đây sau
        });
    }

    @Override
    public int getItemCount() { return userList != null ? userList.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserRole;
        MaterialButton btnBan, btnDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            btnBan = itemView.findViewById(R.id.btnBan);
            btnDetails = itemView.findViewById(R.id.btnDetails);
        }
    }
}