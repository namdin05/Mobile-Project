package com.melodix.app.Service;

import com.melodix.app.Model.Song;
import com.melodix.app.Model.StatusUpdateRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface SongAPIService {

    @GET("artist_songs_view")
    Call<List<Song>> getAllSongs();

    @GET("song_details_view?status=eq.approved&order=created_at.desc")
    Call<List<Song>> getNewReleaseSongs( // quy dinh sau khi thuc hien Call thi se tra ve Song
            @Query("limit") int limit
    );
    @GET("trending_songs_view")
    Call<List<Song>> getTrendingSongs(
            @Query("limit") int limit
    );

    @GET("songs?select=*")
    Call<List<Song>> getSongsByAlbum(
            @Query(value = "album_id", encoded = true) String albumIdFilter // Truyền "eq.MÃ_ALBUM" vào đây
    );

    @PATCH("songs")
    Call<Void> updateRequestStatus(
            @Query("id") String idFilter,
            @Body StatusUpdateRequest body
    );


    // Gọi hàm RPC từ Supabase bằng phương thức POST
    @retrofit2.http.POST("rpc/get_songs_by_genre")
    Call<List<Song>> getSongsByGenre(
            @retrofit2.http.Body java.util.Map<String, Integer> body
    );

    // ĐỔI TÊN ĐƯỜNG DẪN Ở ĐÂY 👇
    @retrofit2.http.POST("rpc/add_song_stream") // Bỏ rest/v1 vì BaseURL của SupabaseClient đã có rồi
    retrofit2.Call<Void> recordPlay(
            @retrofit2.http.Body java.util.Map<String, Object> body
    );
}
