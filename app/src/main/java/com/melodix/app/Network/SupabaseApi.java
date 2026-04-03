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

    // 1. TÌM KIẾM
    // CHÚ Ý: Đã thêm &status=eq.approved để chỉ tìm những bài hát đã được duyệt
    @GET("song_details_view?select=*&status=eq.approved")
    Call<List<Song>> searchSongs(@Query(value = "fts", encoded = true) String ftsQuery);

    @GET("artist_search_view?select=*")
    Call<List<Artist>> searchArtists(@Query(value = "fts", encoded = true) String ftsQuery);

    @GET("album_details_view?select=*")
    Call<List<Album>> searchAlbums(@Query(value = "fts", encoded = true) String ftsQuery);

    @GET("playlists?select=*")
    Call<List<Playlist>> searchPlaylists(@Query(value = "fts", encoded = true) String ftsQuery);

    // 2. CHI TIẾT ALBUM
    @GET("album_details_view?select=*")
    Call<List<Album>> getAlbumById(@Query("id") String idQuery);

    // CHÚ Ý: Chỉ lấy các bài hát đã duyệt trong Album
    @GET("song_details_view?select=*&status=eq.approved")
    Call<List<Song>> getSongsByAlbumId(@Query("album_id") String albumIdQuery);


    // 3. CHI TIẾT NGHỆ SĨ
    @GET("artist_search_view?select=*")
    Call<List<Artist>> getArtistByIdAPI(@Query("id") String idQuery);

    // CHÚ Ý: Chỉ lấy bài hát đã duyệt của Nghệ sĩ
    @GET("artist_songs_view?select=*&status=eq.approved")
    Call<List<Song>> getSongsByArtistId(@Query("artist_id") String artistIdQuery);

    // Lấy Album của Nghệ sĩ (Album thì không có status nên để nguyên)
    @GET("album_details_view?select=*")
    Call<List<Album>> getAlbumsByArtistId(@Query("artist_id") String artistIdQuery);

    // Lấy Nghệ sĩ liên quan (Loại trừ ID hiện tại, giới hạn 5 người)
    @GET("artist_search_view?select=*")
    Call<List<Artist>> getRelatedArtists(@Query("id") String excludeIdQuery, @Query("limit") int limit);
}