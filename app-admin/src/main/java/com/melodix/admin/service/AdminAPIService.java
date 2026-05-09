package com.melodix.admin.service;

import com.melodix.admin.model.AppMetric;
import com.melodix.admin.model.ArtistRequest;
import com.melodix.admin.model.AuditLog;
import java.util.List;
import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface AdminAPIService {
    @GET("audit_logs?select=*&order=created_at.desc")
    Call<List<AuditLog>> getAuditLogs(@Query("limit") int limit, @Query("offset") int offset);

    @GET("app_metrics")
    Call<List<AppMetric>> getAppMetrics();

    @GET("user_request_to_artist?status=eq.pending&select=*,user_profile:profiles(*)")
    Call<List<ArtistRequest>> getPendingArtistRequests();

    @PATCH("user_request_to_artist")
    Call<ResponseBody> updateArtistRequestStatus(@Query("id") String idFilter, @Body Map<String, Object> body);

    @PATCH("profiles")
    Call<ResponseBody> updateUserRole(@Query("id") String idFilter, @Body Map<String, Object> body);
}