package com.melodix.app.Service;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface StorageAPIService {
    @POST("{bucketName}/{fileName}")
    Call<ResponseBody> uploadFileToStorage(
            @Header("Content-Type") String contentType,
            @Header("x-upsert") String upsert,
            @Path("bucketName") String bucketName,
            @Path("fileName") String fileName,
            @Body RequestBody file
    );
}
