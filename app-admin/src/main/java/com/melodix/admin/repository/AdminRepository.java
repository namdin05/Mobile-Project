package com.melodix.admin.repository;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;
import com.melodix.admin.service.AdminAPIService;
import com.melodix.admin.model.AppMetric;
import com.melodix.admin.model.ArtistRequest;
import com.melodix.admin.model.AuditLog;
import com.melodix.core.service.RetrofitClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRepository {
    private AdminAPIService apiService;

    public AdminRepository(Context context) {
        apiService = RetrofitClient.getClient(context).create(AdminAPIService.class);
    }

    public void fetchAuditLogsPaged(int limit, int offset, Callback<List<AuditLog>> callback) {
        apiService.getAuditLogs(limit, offset).enqueue(callback);
    }

    public MutableLiveData<List<AppMetric>> fetchAllAppMetrics() {
        MutableLiveData<List<AppMetric>> appMetricLiveData = new MutableLiveData<>();
        apiService.getAppMetrics().enqueue(new Callback<List<AppMetric>>() {
            @Override
            public void onResponse(Call<List<AppMetric>> call, Response<List<AppMetric>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    appMetricLiveData.setValue(response.body());
                } else {
                    appMetricLiveData.setValue(null);
                }
            }
            @Override
            public void onFailure(Call<List<AppMetric>> call, Throwable t) {
                appMetricLiveData.setValue(null);
            }
        });
        return appMetricLiveData;
    }

    public MutableLiveData<List<ArtistRequest>> fetchPendingArtistRequests() {
        MutableLiveData<List<ArtistRequest>> artistRequests = new MutableLiveData<>();
        apiService.getPendingArtistRequests().enqueue(new Callback<List<ArtistRequest>>() {
            @Override
            public void onResponse(Call<List<ArtistRequest>> call, Response<List<ArtistRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    artistRequests.setValue(response.body());
                } else {
                    artistRequests.setValue(null);
                }
            }
            @Override
            public void onFailure(Call<List<ArtistRequest>> call, Throwable t) {
                artistRequests.setValue(null);
            }
        });
        return artistRequests;
    }

    public void processArtistRequest(ArtistRequest request, String newStatus,
                                     MutableLiveData<Boolean> isSuccess, MutableLiveData<String> message) {
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("status", newStatus);

        apiService.updateArtistRequestStatus("eq." + request.getId(), reqBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    if ("approved".equals(newStatus)) {
                        Map<String, Object> roleBody = new HashMap<>();
                        roleBody.put("role", "artist");
                        apiService.updateUserRole("eq." + request.getUserId(), roleBody).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> roleResp) {
                                if (roleResp.isSuccessful()) {
                                    message.postValue("Đã duyệt thành công và nâng cấp tài khoản!");
                                    isSuccess.postValue(true);
                                } else {
                                    message.postValue("Duyệt thành công nhưng lỗi nâng cấp quyền!");
                                    isSuccess.postValue(false);
                                }
                            }
                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                message.postValue("Lỗi mạng khi nâng cấp quyền!");
                                isSuccess.postValue(false);
                            }
                        });
                    } else {
                        message.postValue("Đã từ chối yêu cầu.");
                        isSuccess.postValue(true);
                    }
                } else {
                    message.postValue("Lỗi từ chối/duyệt yêu cầu: " + response.code());
                    isSuccess.postValue(false);
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                message.postValue("Lỗi mạng!");
                isSuccess.postValue(false);
            }
        });
    }
}