package com.melodix.app.Service;

import com.melodix.app.Model.Profile;

import java.util.List;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProfileAPIService  {

    @GET("profiles")
    Call<List<Profile>> getAllProfiles();

    // Lấy thông tin của 1 user cụ thể dựa vào ID
    @GET("profiles")
    Call<List<Profile>> getProfileById(
            @Query("id") String idFilter // Truyền vào dạng "eq.uuid-của-admin"
    );

    // 2. Hàm Cập nhật (PATCH) thông tin profile
    @PATCH("profiles")
    Call<ResponseBody> updateProfile(
            @Query("id") String idFilter, // eq.[UID]
            @Body java.util.Map<String, Object> bodyData // Gửi HashMap cho tiện
    );

    @PATCH("profiles")
    Call<Void> updateFcmToken(
            @Query(value = "id", encoded = true) String idFilter, // THÊM encoded = true VÀO ĐÂY
            @Body Map<String, Object> bodyData
    );

}
