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
import com.google.android.material.button.MaterialButton;
import com.melodix.app.Model.Profile;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.View.adapters.SongAdapter;

import java.util.ArrayList;
import java.util.List;


public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileHolder> {

    private final Context context;
    private final List<Profile> profileList;
    private final OnProfileActionListener action;

    public ProfileAdapter(Context context, List<Profile> profiles, ProfileAdapter.OnProfileActionListener action) {
        this.context = context;
        this.profileList = profiles;
        this.action = action;
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

        // Viết hoa chữ cái đầu của Role (VD: "artist" -> "Artist")
        String roleStr = user.getRole().substring(0, 1).toUpperCase() + user.getRole().substring(1);
        holder.tvRole.setText(roleStr);

        holder.btnMore.setOnClickListener(v -> showMenu(v, user, position));
    }

    @Override
    public int getItemCount() { return profileList != null ? profileList.size() : 0; }

    static class ProfileHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvRole, tvEmail;
        ImageButton btnMore;

        ProfileHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_avatar);
            tvName = itemView.findViewById(R.id.tv_name);
            tvRole = itemView.findViewById(R.id.tv_role);
            tvEmail = itemView.findViewById(R.id.tv_email);
            btnMore = itemView.findViewById(R.id.btn_more);
        }
    }

    private void showMenu(View anchor, Profile profile, int position) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetTheme);

        View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.dialog_admin_profile_menu, null);
        bottomSheet.setContentView(bottomSheetView);

        // Xử lý nền trong suốt để viền bo góc của bg_card hiện ra
        View parent = (View) bottomSheetView.getParent();
        if (parent != null) {
            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        // Gắn sự kiện click cho từng dòng menu
        bottomSheetView.findViewById(R.id.menu_view).setOnClickListener(v -> {
            if (action != null) action.onMenuClick(profile, position, "view");
            bottomSheet.dismiss();
        });

        bottomSheetView.findViewById(R.id.menu_ban).setOnClickListener(v -> {
            if (action != null) action.onMenuClick(profile, position, "ban");
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }

    public void update(ArrayList<Profile> newProfiles) {
        if (newProfiles != null)
        {
            this.profileList.clear();
            this.profileList.addAll(newProfiles);
            notifyDataSetChanged();
        }

    }
}