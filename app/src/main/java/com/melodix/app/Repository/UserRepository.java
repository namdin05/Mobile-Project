//package com.melodix.app.Repository;
//
//import android.content.Context;
//import com.melodix.app.Model.AppUser;
//import com.melodix.app.Model.Playlist;
//import com.melodix.app.Model.SearchResultItem;
//
//import java.util.ArrayList;
//
//public class UserRepository {
//    private final AppRepository repository;
//
//    public UserRepository(Context context) {
//        repository = AppRepository.getInstance(context);
//    }
//
//    public AppUser currentUser() {
//        return repository.getCurrentUser();
//    }
//
//    public ArrayList<Playlist> playlists() {
//        return repository.getCurrentUserPlaylists();
//    }
//
//    // ĐÃ SỬA: Chuyển thành void và thêm SearchCallback để truyền đi tiếp
//    public void searchAll(String keyword, String filter, AppRepository.SearchCallback callback) {
//        repository.search(keyword, filter, callback);
//    }
//
//    public ArrayList<String> recentSearches() {
//        return repository.getRecentSearches();
//    }
//
//    public void clearRecentSearches() {
//        repository.clearRecentSearches();
//    }
//}