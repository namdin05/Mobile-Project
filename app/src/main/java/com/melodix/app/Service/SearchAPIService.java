package com.melodix.app.Service;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.Song;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SearchAPIService {
    @GET("song_details_view?select=*&status=eq.approved")
    Call<List<Song>> searchSongs(@Query(value = "fts", encoded = true) String ftsQuery);

    // THÊM DÒNG NÀY VÀO ĐÂY LÀ XONG
    @GET("song_details_view?select=*")
    Call<List<Song>> getSongById(@Query("id") String id);

    @GET("artist_search_view?select=*")
    Call<List<Artist>> searchArtists(@Query(value = "fts", encoded = true) String ftsQuery);

    @GET("album_details_view?select=*")
    Call<List<Album>> searchAlbums(@Query(value = "fts", encoded = true) String ftsQuery);

    @GET("playlists?select=*")
    Call<List<Playlist>> searchPlaylists(@Query(value = "fts", encoded = true) String ftsQuery);
}