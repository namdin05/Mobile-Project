    package com.melodix.app.Service;

    import com.melodix.app.Model.Album;
    import com.melodix.app.Model.Artist;
    import com.melodix.app.Model.ArtistStats;
    import com.melodix.app.Model.Song;
    import com.melodix.app.Model.SongRequestUpload;

    import java.util.List;
    import java.util.Map;

    import okhttp3.RequestBody;
    import okhttp3.ResponseBody;
    import retrofit2.Call;
    import retrofit2.http.Body;
    import retrofit2.http.DELETE;
    import retrofit2.http.GET;
    import retrofit2.http.Header;
    import retrofit2.http.PATCH;
    import retrofit2.http.POST;
    import retrofit2.http.Path;
    import retrofit2.http.Query;

    public interface ArtistAPIService {

        // 3. GỌI HÀM RPC ĐỂ TỰ ĐỘNG LƯU BÀI HÁT & DANH SÁCH CA SĨ COLLAB
        @POST("rpc/upload_song_with_artists")
        Call<Void> submitSongWithArtists(
                @Body SongRequestUpload body
        );

        @POST("albums")
        Call<ResponseBody> createAlbum(
                @Body Map<String, Object> albumData
        );

        @DELETE("songs")
        Call<Void> deleteSong(@Query("id") String operatorAndId);

        @PATCH("songs")
        Call<ResponseBody> updateSong(@Query("id") String operatorAndId, @Body Map<String, Object> songData);

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
        Call<List<Song>> getMyUploadSongs(@Query("artist_id") String artistIdQuery);

        // Lấy thống kê của 1 nghệ sĩ
        @GET("artist_stats_view?select=*")
        Call<List<ArtistStats>> getArtistStats(@Query("artist_id") String artistIdQuery);
        // NÂNG CẤP: Gọi hàm RPC để tạo album và thêm bài hát cùng lúc
        @POST("rpc/create_album_with_songs")
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
        // Lệnh xóa file mp3 khỏi bucket "song"
        @retrofit2.http.DELETE("storage/v1/object/song/{file_name}")
        Call<Void> deleteAudioFile(
                @retrofit2.http.Path("file_name") String fileName
        );

        // Lệnh xóa file ảnh khỏi bucket "cover_song"
        @retrofit2.http.DELETE("storage/v1/object/cover_song/{file_name}")
        Call<Void> deleteCoverFile(
                @retrofit2.http.Path("file_name") String fileName
        );

        // Gửi lệnh xóa Album
        @retrofit2.http.DELETE("albums")
        Call<Void> deleteAlbum(@retrofit2.http.Query("id") String operatorAndId);
    }