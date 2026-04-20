package com.melodix.app.View.admin.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Adapter.ProfileAdapter;
import com.melodix.app.Model.Profile;
import com.melodix.app.R;
import com.melodix.app.Utils.ProfileActionHelper;
import com.melodix.app.ViewModel.admin.AdminUserViewModel;

import java.util.ArrayList;
import java.util.List;

public class UserManagementFragment extends Fragment {

    private RecyclerView rvAllUsers;
    private ProfileAdapter profileAdapter;
    private AutoCompleteTextView actvRole;

    private AdminUserViewModel viewModel;

    // Cần 2 danh sách: 1 cái chứa TOÀN BỘ dữ liệu gốc, 1 cái để HIỂN THỊ theo filter
    private List<Profile> fullUserList = new ArrayList<>();
    private List<Profile> displayList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAllUsers = view.findViewById(R.id.rvAllUsers);
        rvAllUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        actvRole = view.findViewById(R.id.actvRole);

        setupRecyclerView();

        setupRoleFilter();

        viewModel = new ViewModelProvider(this).get(AdminUserViewModel.class);
        viewModel.getAllProfiles().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                this.fullUserList = songs;
                this.displayList = new ArrayList<>(songs);

                // Xử lý bộ lọc ngầm: Khi có data mới, phải lọc lại theo Dropdown hiện tại
                String currentFilter = actvRole.getText().toString();
                filterSongsByStatus(currentFilter); // Thay bằng hàm lọc của bạn nếu tên khác
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        String[] statuses = {"All", "User", "Artist", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item,
                statuses
        );
        actvRole.setAdapter(adapter);
    }

    private void setupRecyclerView() {
        // Đã khắc phục lỗi trống logic click ở đây
        profileAdapter = new ProfileAdapter(requireContext(), new ArrayList<>(), new ProfileAdapter.OnProfileActionListener() {
            @Override
            public void onMenuClick(Profile profile, int position, String action) {
                ProfileActionHelper.handleMenuClick(requireContext(), profile, action);
            }
        });

        rvAllUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAllUsers.setAdapter(profileAdapter);
    }

    private void setupRoleFilter() {
        actvRole.setOnItemClickListener((parent, view, position, id) -> {
            String selectedStatus = parent.getItemAtPosition(position).toString();
            filterSongsByStatus(selectedStatus);
        });
    }


    private void filterSongsByStatus(String status) {
        if (status.equals("All")) {
            displayList = new ArrayList<>(fullUserList);
        } else {
            displayList = new ArrayList<>();
            for (Profile profile : fullUserList) {
                if (profile.getRole() != null && profile.getRole().equalsIgnoreCase(status)) {
                    displayList.add(profile);
                }
            }
        }
        // Chỉ dùng 1 hàm update duy nhất, KHÔNG tạo lại Adapter
        profileAdapter.update(new ArrayList<>(displayList));
    }


}
