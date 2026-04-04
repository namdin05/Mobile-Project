package com.melodix.app.Service;

import com.melodix.app.Model.Profile;

import java.util.List;

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

public interface StorgeAPIService {

    @POST("storage/v1/object/{bucketName}/{fileName}")
    Call<ResponseBody> uploadFileToStorage(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Header("Content-Type") String contentType,
            @Header("x-upsert") String upsert,
            @Path("bucketName") String bucketName, // <-- Thêm biến này
            @Path("fileName") String fileName,
            @Body RequestBody file
    );

}
