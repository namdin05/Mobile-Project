package com.melodix.app.View.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.melodix.app.Model.AppUser;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.ResourceUtils;

public class AccountFragment extends Fragment {
    private AppRepository repository;
    private ImageView avatar;
    private TextView name;
    private TextView headline;
    private Switch dark;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        repository = AppRepository.getInstance(requireContext());

        avatar = view.findViewById(R.id.img_avatar);
        name = view.findViewById(R.id.tv_name);
        headline = view.findViewById(R.id.tv_headline);
        dark = view.findViewById(R.id.switch_dark_mode);

        dark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                // Tạm thời chỉ đổi UI hiển thị, không lưu vào Repo để tránh lỗi
                if (isChecked) {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (repository != null) {
            AppUser user = repository.getCurrentUser();
            if (user != null) {
                avatar.setImageResource(ResourceUtils.anyDrawable(requireContext(), user.avatarRes));
                name.setText(user.displayName);
                headline.setText(user.headline);
                dark.setChecked(user.darkMode);
            }
        }
    }
}