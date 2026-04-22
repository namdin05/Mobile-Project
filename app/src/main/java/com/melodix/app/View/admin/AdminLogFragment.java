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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Adapter.AdminLogAdapter;
import com.melodix.app.Adapter.ProfileAdapter;
import com.melodix.app.BuildConfig;
import com.melodix.app.Model.AuditLog;
import com.melodix.app.Model.Profile;
import com.melodix.app.R;
import com.melodix.app.Service.AdminAPIService;
import com.melodix.app.Service.RetrofitClient;
import com.melodix.app.Utils.ProfileActionHelper;
import com.melodix.app.ViewModel.admin.AdminLogViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminLogFragment extends Fragment {

    private RecyclerView rvAuditLogs;
    private AdminLogAdapter logAdapter;
    private List<AuditLog> logList;

    private AdminLogViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_log, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAuditLogs = view.findViewById(R.id.rvAuditLogs);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvAuditLogs.setLayoutManager(layoutManager);

        logList = new ArrayList<>();
        logAdapter = new AdminLogAdapter(logList);
        rvAuditLogs.setAdapter(logAdapter);

        viewModel = new ViewModelProvider(this).get(AdminLogViewModel.class);

        // 1. Lắng nghe dữ liệu đổ về
        viewModel.getAuditLogs().observe(getViewLifecycleOwner(), auditLogs -> {
            if (auditLogs != null) {
                logList.clear();
                logList.addAll(auditLogs);
                logAdapter.notifyDataSetChanged();
            }
        });

        // 2. Kích hoạt lấy 20 dòng ĐẦU TIÊN khi vừa mở màn hình
        viewModel.loadMoreLogs();

        // 3. LẮP CẢM BIẾN CUỘN
        rvAuditLogs.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // dy > 0 có nghĩa là người dùng đang vuốt lên (cuộn danh sách xuống dưới)
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    // Nếu vị trí đang xem + số item hiển thị >= tổng số item -> Đã đến đáy
                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                        viewModel.loadMoreLogs();
                    }
                }
            }
        });
    }
}