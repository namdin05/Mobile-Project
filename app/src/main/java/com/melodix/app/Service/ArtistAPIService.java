    package com.melodix.app.Service;

    import com.melodix.app.Model.Album;
    import com.melodix.app.Model.Artist;
    import com.melodix.app.Model.ArtistStats;
    import com.melodix.app.Model.Song;
    import com.melodix.app.Model.SongRequestUpload;

    import java.util.List;

    import okhttp3.RequestBody;
    import retrofit2.Call;
    import retrofit2.http.Body;
    import retrofit2.http.GET;
    import retrofit2.http.Header;
    import retrofit2.http.POST;
    import retrofit2.http.Path;
    import retrofit2.http.Query;

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

        // Thêm vào ArtistAPIService.java
        @POST("rest/v1/albums")
        Call<okhttp3.ResponseBody> createAlbum(
                @Header("apikey") String apiKey,
                @Header("Authorization") String token,
                @Body java.util.Map<String, Object> albumData
        );
        // Gửi lệnh DELETE lên bảng songs dựa vào ID
        @retrofit2.http.DELETE("songs")
        Call<Void> deleteSong(@Query("id") String operatorAndId);
        // Mình truyền chuỗi "eq.id_của_bài_hát" vào đây

        // Gửi lệnh PATCH lên bảng songs để cập nhật thông tin
        @retrofit2.http.PATCH("songs")
        Call<okhttp3.ResponseBody> updateSong(@Query("id") String operatorAndId, @retrofit2.http.Body java.util.Map<String, Object> songData);

        // ... các hàm upload cũ của bạn giữ nguyên ...

        @GET("artist_search_view?select=*")
        Call<List<Artist>> getArtistByIdAPI(@Query("id") String idQuery);

        @GET("artist_songs_view?select=*&status=eq.approved")
        Call<List<Song>> getSongsByArtistId(@Query("artist_id") String artistIdQuery);

        @GET("album_details_view?select=*")
        Call<List<Album>> getAlbumsByArtistId(@Query("artist_id") String artistIdQuery);

        // NẾU CÓ HÀM NÀY CHO KHÁN GIẢ THÌ PHẢI CHẶN LẠI:
        @GET("album_details_view?select=*&status=eq.approved")
        Call<List<Album>> getAlbumsForPublic(@Query("artist_id") String artistIdQuery);

        @GET("artist_search_view?select=*")
        Call<List<Artist>> getRelatedArtists(@Query("id") String excludeIdQuery, @Query("limit") int limit);

        @GET("artist_songs_view?select=*")
        Call<List<com.melodix.app.Model.Song>> getMyUploadSongs(@Query("artist_id") String artistIdQuery);

        // Lấy thống kê của 1 nghệ sĩ
        @GET("artist_stats_view?select=*")
        Call<List<ArtistStats>> getArtistStats(@Query("artist_id") String artistIdQuery);
        // NÂNG CẤP: Gọi hàm RPC để tạo album và thêm bài hát cùng lúc
        @POST("rest/v1/rpc/create_album_with_songs")
        Call<okhttp3.ResponseBody> createAlbumWithSongs(
                @Header("apikey") String apiKey,
                @Header("Authorization") String token,
                @Body java.util.Map<String, Object> albumData
        );

        @retrofit2.http.PATCH("albums")
        Call<okhttp3.ResponseBody> updateAlbum(
                @retrofit2.http.Query("id") String operatorAndId,
                @retrofit2.http.Body java.util.Map<String, Object> albumData
        );

        // Thêm hàm này vào để ép gửi JsonObject có chứa JsonNull
        // Dùng RequestBody của OkHttp để cấm Retrofit tự động can thiệp dữ liệu
        @retrofit2.http.PATCH("songs")
        Call<okhttp3.ResponseBody> removeSongFromAlbumRaw(
                @retrofit2.http.Query("id") String operatorAndId,
                @retrofit2.http.Body okhttp3.RequestBody body
        );

        // Hàm gọi RPC để Cập nhật Album và Danh sách bài hát cùng lúc
        @retrofit2.http.POST("rpc/update_album_with_songs")
        Call<okhttp3.ResponseBody> updateAlbumWithSongs(
                @retrofit2.http.Body java.util.Map<String, Object> bodyData
        );
        // Gọi HEAD request cực nhẹ để lấy đúng con số Count, không tải data thừa
        @retrofit2.http.HEAD("follows")
        Call<Void> getFollowerCount(
                @retrofit2.http.Header("Prefer") String preferCount, // Bắt buộc truyền "count=exact"
                @retrofit2.http.Query("artist_id") String artistIdQuery
        );
    }