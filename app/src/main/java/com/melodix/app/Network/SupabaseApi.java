package com.melodix.app.Network;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.Song;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SupabaseApi {

    // CHÚ Ý: Đã thêm encoded = true để Retrofit không tự ý làm hỏng câu lệnh FTS
    @GET("song_details_view?select=*")
    Call<List<Song>> searchSongs(@Query(value = "fts", encoded = true) String ftsQuery);

    @GET("artist_search_view?select=*")
    Call<List<Artist>> searchArtists(@Query(value = "fts", encoded = true) String ftsQuery);

    @GET("album_details_view?select=*")
    Call<List<Album>> searchAlbums(@Query(value = "fts", encoded = true) String ftsQuery);

    @GET("playlists?select=*")
    Call<List<Playlist>> searchPlaylists(@Query(value = "fts", encoded = true) String ftsQuery);

    @GET("album_details_view?select=*")
    Call<List<Album>> getAlbumById(@Query("id") String idQuery);

    @GET("song_details_view?select=*")
    Call<List<Song>> getSongsByAlbumId(@Query("album_id") String albumIdQuery);

    @GET("artist_search_view?select=*")
    Call<List<Artist>> getArtistByIdAPI(@Query("id") String idQuery);
}