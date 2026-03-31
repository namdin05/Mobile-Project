package com.melodix.app.Service;

import com.melodix.app.Model.SongRequest;
import com.melodix.app.Model.StatusUpdateRequest;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface AdminAPIService {

    // Lệnh Join cực hay của Supabase
    @GET("rest/v1/song_requests?status=eq.pending&select=*,profiles(display_name)")
    Call<List<SongRequest>> getPendingRequests(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token
    );

    @PATCH("rest/v1/song_requests")
    Call<Void> updateRequestStatus(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Query("id") String idFilter,
            @Body StatusUpdateRequest body
    );
}