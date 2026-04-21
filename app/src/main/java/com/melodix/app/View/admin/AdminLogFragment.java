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

        rvAuditLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        logList = new ArrayList<>();
        logAdapter = new AdminLogAdapter(logList);
        rvAuditLogs.setAdapter(logAdapter);

        viewModel = new ViewModelProvider(this).get(AdminLogViewModel.class);
        viewModel.fetchAuditLogs().observe(getViewLifecycleOwner(), auditLogs -> {
            if (auditLogs != null) {
                logList.clear();
                logList.addAll(auditLogs);
                logAdapter.notifyDataSetChanged();
            }

            if(logList.isEmpty()){
                Toast.makeText(getContext(), "Chưa có dữ liệu log nào", Toast.LENGTH_SHORT).show();
            }
        });

    }
}