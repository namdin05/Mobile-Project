package com.melodix.app.Data;

import com.melodix.app.Model.Album;
import com.melodix.app.Model.AppUser;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.Playlist;
import com.melodix.app.Model.Song;
import com.melodix.app.Utils.Constants;
import com.melodix.app.Utils.SecurityUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class MockDatabase {

    public static class DataState {
        // Chỉ giữ lại mảng users có dữ liệu. Các mảng còn lại để rỗng
        // nhằm tránh lỗi Crash khi AppRepository lỡ gọi tới.
        public ArrayList<AppUser> users = new ArrayList<>();
        public ArrayList<Artist> artists = new ArrayList<>();
        public ArrayList<Album> albums = new ArrayList<>();
        public ArrayList<Song> songs = new ArrayList<>();
        public ArrayList<Playlist> playlists = new ArrayList<>();
    }

    public static DataState createDefaultState(android.content.Context context) {
        DataState state = new DataState();

        // 1. DỮ LIỆU USER (BẮT BUỘC GIỮ LẠI: Phục vụ tính năng lưu Lịch sử tìm kiếm Local)
        AppUser user = new AppUser("user_listener", "Alex Rivera", "alex", "user@melodix.app",
                SecurityUtils.sha256("123456"), Constants.ROLE_USER, "avatar_listener_1",
                "Good evening", "Music lover, playlist collector.");
        user.offlineMode = false;

        // Mock sẵn vài từ khóa lịch sử tìm kiếm để test UI Search
        user.recentSearches.addAll(Arrays.asList("Lo-fi Beats", "Alex Rivers", "Late Night"));

        state.users.add(user);

        // ĐÃ XÓA TOÀN BỘ CODE TẠO ARTIST, ALBUM, SONG VÀ PLAYLIST MẪU

        return state;
    }
}