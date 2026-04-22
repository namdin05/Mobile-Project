package com.melodix.app.Service;

import com.melodix.app.Model.AppMetric;
import com.melodix.app.Model.ArtistRequest;
import com.melodix.app.Model.AuditLog;
import com.melodix.app.Model.Genre;
import com.melodix.app.Model.Profile;
import com.melodix.app.Model.Song;
import com.melodix.app.Model.StatusUpdateRequest;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface AdminAPIService {
    @GET("app_metrics?select=*")
    Call<List<AppMetric>> getAppMetrics();

    @GET("audit_logs?select=*&order=changed_at.desc")
    Call<List<AuditLog>> getAuditLogs(
            @Query("limit") int limit,
            @Query("offset") int offset
    );
    @GET("user_request_to_artist?select=*,profiles(*)&status=eq.pending")
    Call<List<ArtistRequest>> getPendingArtistRequests();

    // Cập nhật trạng thái duyệt/từ chối
    @PATCH("user_request_to_artist")
    Call<ResponseBody> updateArtistRequestStatus(
            @Query("id") String idFilter,
            @Body Map<String, Object> body
    );

    // Nâng cấp quyền User lên Artist trong bảng profiles
    @PATCH("profiles")
    Call<ResponseBody> updateUserRole(
            @Query("id") String userIdFilter,
            @Body Map<String, Object> body
    );
}