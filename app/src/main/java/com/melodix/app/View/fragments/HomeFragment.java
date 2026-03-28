package com.melodix.app.View.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.melodix.app.Model.AppUser;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.ResourceUtils;

public class HomeFragment extends Fragment {
    private AppRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        repository = AppRepository.getInstance(requireContext());

        AppUser user = repository.getCurrentUser();
        ImageView avatar = view.findViewById(R.id.img_avatar);
        TextView greeting = view.findViewById(R.id.tv_greeting);
        TextView subGreeting = view.findViewById(R.id.tv_subgreeting);

        if (user != null) {
            avatar.setImageResource(ResourceUtils.anyDrawable(requireContext(), user.avatarRes));
            greeting.setText("Welcome back");
            subGreeting.setText(user.headline == null ? "Good evening" : user.headline);
        }

        // Tạm ẩn các danh sách (Banner, Trending, Genre, New Releases)
        return view;
    }
}