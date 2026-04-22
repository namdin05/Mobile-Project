package com.melodix.app.ViewModel.admin;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.AuditLog;
import com.melodix.app.Repository.admin.AdminRepository;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminLogViewModel extends AndroidViewModel {
    private AdminRepository repository;
    private MutableLiveData<List<AuditLog>> auditLogsLiveData = new MutableLiveData<>();

    private List<AuditLog> currentLogs = new ArrayList<>();
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int currentOffset = 0;
    private final int LIMIT = 20; // Mỗi lần chỉ tải 20 dòng

    public AdminLogViewModel(@NotNull Application application) {
        super(application);
        repository = new AdminRepository(application);
    }

    public LiveData<List<AuditLog>> getAuditLogs() {
        return auditLogsLiveData;
    }

    // Hàm gọi lấy dữ liệu trang tiếp theo
    public void loadMoreLogs() {
        if (isLoading || isLastPage) return; // Đang tải hoặc hết dữ liệu thì chặn lại
        isLoading = true;

        repository.fetchAuditLogsPaged(LIMIT, currentOffset, new Callback<List<AuditLog>>() {
            @Override
            public void onResponse(Call<List<AuditLog>> call, Response<List<AuditLog>> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<AuditLog> newLogs = response.body();

                    if (!newLogs.isEmpty()) {
                        // Cộng dồn dữ liệu mới vào danh sách cũ
                        currentLogs.addAll(newLogs);
                        auditLogsLiveData.setValue(currentLogs);

                        // Tăng mốc offset cho lần tải tiếp theo
                        currentOffset += LIMIT;
                    }

                    // Nếu số lượng tải về ít hơn LIMIT, nghĩa là đã chạm đáy DB
                    if (newLogs.size() < LIMIT) {
                        isLastPage = true;
                    }
                }
            }

            @Override
            public void onFailure(Call<List<AuditLog>> call, Throwable t) {
                isLoading = false;
                // Có thể xử lý thông báo lỗi mạng ở đây nếu cần
            }
        });
    }

    // Tiện ích để reset lại từ đầu khi Admin muốn làm mới (Kéo để refresh)
    public void refreshLogs() {
        currentLogs.clear();
        currentOffset = 0;
        isLastPage = false;
        isLoading = false;
        loadMoreLogs();
    }
}
