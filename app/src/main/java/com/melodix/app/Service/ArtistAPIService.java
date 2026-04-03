package com.melodix.app.Service;

import com.melodix.app.Model.SongRequestUpload;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ArtistAPIService {

    // 1. Upload Ảnh bìa lên Storage (Bucket: cover_song)
    @POST("storage/v1/object/cover_song/{file_name}")
    Call<Void> uploadCover(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Header("Content-Type") String contentType,
            @Path("file_name") String fileName,
            @Body RequestBody fileData
    );

    // 2. Upload Nhạc lên Storage (Bucket: song)
    @POST("storage/v1/object/song/{file_name}")
    Call<Void> uploadAudio(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Header("Content-Type") String contentType,
            @Path("file_name") String fileName,
            @Body RequestBody fileData
    );

    // 3. GỌI HÀM RPC ĐỂ TỰ ĐỘNG LƯU BÀI HÁT & DANH SÁCH CA SĨ COLLAB
    @POST("rest/v1/rpc/upload_song_with_artists")
    Call<Void> submitSongWithArtists(
            @Header("apikey") String apiKey,
            @Header("Authorization") String token,
            @Body SongRequestUpload body
    );
}