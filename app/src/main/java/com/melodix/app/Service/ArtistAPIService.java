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

        @GET("artist_search_view?select=*")
        Call<List<Artist>> getRelatedArtists(@Query("id") String excludeIdQuery, @Query("limit") int limit);

        @GET("artist_songs_view?select=*")
        Call<List<Song>> getMyUploadSongs(@Query("artist_id") String artistIdQuery);

        // Lấy thống kê của 1 nghệ sĩ
        @GET("artist_stats_view?select=*")
        Call<List<ArtistStats>> getArtistStats(@Query("artist_id") String artistIdQuery);


    }