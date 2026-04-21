package com.melodix.app.Repository.admin;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Model.AppMetric;
import com.melodix.app.Model.ArtistRequest;
import com.melodix.app.Model.AuditLog;
import com.melodix.app.Service.AdminAPIService;
import com.melodix.app.Service.RetrofitClient;

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

    public MutableLiveData<List<AuditLog>> fetchAuditLogs() {
        MutableLiveData<List<AuditLog>> auditLog = new MutableLiveData<>();

        apiService.getAuditLogs().enqueue(new Callback<List<AuditLog>>() {
            @Override
            public void onResponse(Call<List<AuditLog>> call, Response<List<AuditLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    auditLog.setValue(response.body());

                } else {
                    Log.e("AdminLog", "Lỗi tải dữ liệu: " + response.code());
                    auditLog.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<AuditLog>> call, Throwable t) {
                Log.e("AdminLog", "Lỗi mạng: " + t.getMessage());
                auditLog.setValue(null);
            }
        });

        return auditLog;
    }

    public MutableLiveData<List<AppMetric>> fetchAllAppMetrics() {
        MutableLiveData<List<AppMetric>> appMetricLiveData = new MutableLiveData<>();

        apiService.getAppMetrics().enqueue(new Callback<List<AppMetric>>() {
            @Override
            public void onResponse(Call<List<AppMetric>> call, Response<List<AppMetric>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 2. CÓ DỮ LIỆU THÌ ĐẨY VÀO HỘP NGAY LẬP TỨC BẰNG setValue()
                    appMetricLiveData.setValue(response.body());
                    Log.e("ADMIN_STAT", "Lấy dữ liệu thống kê thành công: " + response.body().size() + " dòng");

                } else {
                    Log.e("ADMIN_STAT", "Lỗi lấy dữ liệu thống kê: " + response.code());
                    appMetricLiveData.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<AppMetric>> call, Throwable t) {
                Log.e("ADMIN_STAT", "Lỗi mạng: " + t.getMessage());
                appMetricLiveData.setValue(null);
            }
        });

        // 3. Trả cái hộp về cho ViewModel (lúc này hộp rỗng, nhưng 1 giây sau Retrofit chạy xong sẽ tự động bơm data vào)
        return appMetricLiveData;
    }

    public MutableLiveData<List<ArtistRequest>> fetchPendingArtistRequests() {
        MutableLiveData<List<ArtistRequest>> artistRequests = new MutableLiveData<>();

        apiService.getPendingArtistRequests().enqueue(new Callback<List<ArtistRequest>>() {
            @Override
            public void onResponse(Call<List<ArtistRequest>> call, Response<List<ArtistRequest>> response) {
                if (response.isSuccessful() && response.body() != null)
                {
                    artistRequests.setValue(response.body());
                }
                else
                {
                    Log.e("AdminLog", "Lỗi tải dữ liệu: " + response.code());
                    artistRequests.setValue(null);
                }
            }

            @Override
            public void onFailure(Call<List<ArtistRequest>> call, Throwable t) {
                Log.e("AdminLog", "Lỗi mạng: " + t.getMessage());
                artistRequests.setValue(null);
            }
        });

        return artistRequests;
    }

    public void processArtistRequest(ArtistRequest request, String newStatus,
                                     MutableLiveData<Boolean> isSuccess, MutableLiveData<String> message) {

        // 1. Cập nhật bảng user_request_to_artist
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("status", newStatus);

        apiService.updateArtistRequestStatus("eq." + request.getId(), reqBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {

                    // 2. Nếu Approve, gọi tiếp API để đổi quyền trong bảng profiles
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
                        // Nếu chỉ Reject thì báo thành công luôn
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
