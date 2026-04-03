package com.melodix.app.Service;

import com.melodix.app.Model.Song;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface SongAPIService {
    @GET("rest/v1/song_details_view?status=eq.approved&order=created_at.desc")
    Call<List<Song>> getNewReleaseSongs( // quy dinh sau khi thuc hien Call thi se tra ve Song
            @Header("apikey") String apiKey,
            @Query("limit") int limit
    );
    @GET("rest/v1/trending_songs_view")
    Call<List<Song>> getTrendingSongs(
            @Header("apikey") String apiKey,
            @Query("limit") int limit
    );
}
