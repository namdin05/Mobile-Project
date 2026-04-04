package com.melodix.app.View.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.melodix.app.Adapter.AdminUserAdapter;
import com.melodix.app.BuildConfig;
import com.melodix.app.Model.Profile;
import com.melodix.app.R;
import com.melodix.app.Service.AdminAPIService;
import com.melodix.app.Service.RetrofitClient;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUserFragment extends Fragment {

    private RecyclerView rvAllUsers;
    private AdminUserAdapter adapter;

    // Cần 2 danh sách: 1 cái chứa TOÀN BỘ dữ liệu gốc, 1 cái để HIỂN THỊ theo filter
    private List<Profile> fullUserList = new ArrayList<>();
    private List<Profile> displayList = new ArrayList<>();

    private MaterialButton btnFilterAll, btnFilterUsers, btnFilterArtists;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAllUsers = view.findViewById(R.id.rvAllUsers);
        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnFilterUsers = view.findViewById(R.id.btnFilterUsers);
        btnFilterArtists = view.findViewById(R.id.btnFilterArtists);

        rvAllUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminUserAdapter(displayList);
        rvAllUsers.setAdapter(adapter);

        setupFilters();
        fetchAllUsers();
    }

    private void fetchAllUsers() {
        AdminAPIService apiService = RetrofitClient.getClient().create(AdminAPIService.class);
        String token = "Bearer " + BuildConfig.SERVICE_KEY;

        apiService.getAllUsers(BuildConfig.SERVICE_KEY, token).enqueue(new Callback<List<Profile>>() {
            @Override
            public void onResponse(Call<List<Profile>> call, Response<List<Profile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullUserList.clear();
                    fullUserList.addAll(response.body());
                    filterData("all"); // Mặc định hiển thị tất cả
                }
            }
            @Override
            public void onFailure(Call<List<Profile>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi tải User", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==== LOGIC FILTER XỊN SÒ ====
    private void setupFilters() {
        btnFilterAll.setOnClickListener(v -> filterData("all"));
        btnFilterUsers.setOnClickListener(v -> filterData("user"));
        btnFilterArtists.setOnClickListener(v -> filterData("artist"));
    }

    private void filterData(String roleType) {
        displayList.clear();
        if (roleType.equals("all")) {
            displayList.addAll(fullUserList);
        } else {
            for (Profile user : fullUserList) {
                if (user.getRole().equals(roleType)) {
                    displayList.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();

        // MVP: Đổi màu nút để biết đang ở Tab nào (Bạn tự code thêm đổi màu background nhé)
    }
}