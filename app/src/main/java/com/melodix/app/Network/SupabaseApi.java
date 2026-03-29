package com.melodix.app.Network; // Đổi lại package cho đúng với project của bạn

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.Song;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SupabaseApi {

    // Tìm bài hát
    @GET("songs?select=*")
    Call<List<Song>> searchSongs(@Query("fts") String ftsQuery);

    // Tìm nghệ sĩ (thông qua Bảng ảo View)
    @GET("artist_search_view?select=*")
    Call<List<Artist>> searchArtists(@Query("fts") String ftsQuery);

    // Tìm album
    @GET("albums?select=*")
    Call<List<Album>> searchAlbums(@Query("fts") String ftsQuery);

    // Tìm playlist
    @GET("playlists?select=*")
    Call<List<Playlist>> searchPlaylists(@Query("fts") String ftsQuery);
}