package com.melodix.app.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.melodix.app.Model.Profile;
import com.melodix.app.R;
import com.melodix.app.Repository.AuthRepository;

import java.util.ArrayList;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileHolder> {

    private final Context context;
    private final List<Profile> profileList;
    private final OnProfileActionListener action;
    private final AuthRepository authRepository;

    public ProfileAdapter(Context context, List<Profile> profiles, OnProfileActionListener action) {
        this.context = context;
        this.profileList = profiles;
        this.action = action;
        this.authRepository = new AuthRepository(context);
    }

    public interface OnProfileActionListener {
        void onMenuClick(Profile profile, int position, String actionId);
    }

    @NonNull
    @Override
    public ProfileHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new ProfileHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileHolder holder, int position) {
        Profile user = profileList.get(position);
        holder.tvName.setText(user.getDisplayName());

        String roleStr = user.getRole() != null ? user.getRole().substring(0, 1).toUpperCase() + user.getRole().substring(1) : "User";
        holder.tvRole.setText(roleStr);

        Glide.with(context)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(holder.imgAvatar);

        holder.btnMore.setOnClickListener(v -> showMenu(v, user, position));
    }

    @Override
    public int getItemCount() { return profileList != null ? profileList.size() : 0; }

    static class ProfileHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvRole;
        ImageButton btnMore;

        ProfileHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvRole = itemView.findViewById(R.id.tv_role);
            btnMore = itemView.findViewById(R.id.btn_more);
        }
    }

    private void showMenu(View anchor, Profile profile, int position) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetTheme);

        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.dialog_admin_profile_menu, null);
        bottomSheet.setContentView(bottomSheetView);

        TextView btnBan = bottomSheetView.findViewById(R.id.menu_ban);
        btnBan.setEnabled(false);
        btnBan.setText("Checking status...");

        // KIỂM TRA TRẠNG THÁI BAN THỰC TẾ TỪ AUTH TRƯỚC KHI HIỆN MENU
        authRepository.checkUserBanStatus(profile.getId()).observeForever(isBanned -> {
            btnBan.setEnabled(true);
            profile.setBannedUntil(isBanned ? "2099-12-31T23:59:59Z" : "none");
            
            if (isBanned) {
                btnBan.setText("Unban User");
                btnBan.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
            } else {
                btnBan.setText("Ban User");
                btnBan.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        });

        bottomSheetView.findViewById(R.id.menu_view).setOnClickListener(v -> {
            if (action != null) action.onMenuClick(profile, position, "view");
            bottomSheet.dismiss();
        });

        btnBan.setOnClickListener(v -> {
            if (action != null) {
                String actionType = profile.isBanned() ? "unban" : "ban";
                action.onMenuClick(profile, position, actionType);
            }
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }

    public void update(ArrayList<Profile> newProfiles) {
        if (newProfiles != null) {
            this.profileList.clear();
            this.profileList.addAll(newProfiles);
            notifyDataSetChanged();
        }
    }
}
