package com.melodix.app.ViewModel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Data.MockDataStore;
import com.melodix.app.Model.Album;
import com.melodix.app.Model.Artist;
import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.Model.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchViewModel extends ViewModel {

    private static final long SEARCH_DEBOUNCE_DELAY_MS = 400L;

    private final MutableLiveData<List<String>> categoriesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<SearchResultItem>> searchResultsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showCategoriesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> recentSearchesLiveData = new MutableLiveData<>();

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private String currentFilterType = "ALL"; // ALL, SONG, ARTIST, ALBUM

    public SearchViewModel() {
        loadInitialState();
    }

    public LiveData<List<String>> getCategoriesLiveData() { return categoriesLiveData; }
    public LiveData<List<SearchResultItem>> getSearchResultsLiveData() { return searchResultsLiveData; }
    public LiveData<Boolean> getShowCategoriesLiveData() { return showCategoriesLiveData; }
    public LiveData<List<String>> getRecentSearchesLiveData() { return recentSearchesLiveData; }

    private void loadInitialState() {
        categoriesLiveData.setValue(MockDataStore.getGenres());
        searchResultsLiveData.setValue(new ArrayList<SearchResultItem>());
        showCategoriesLiveData.setValue(true);
        loadRecentSearches();
    }

    public void loadRecentSearches() {
        recentSearchesLiveData.setValue(MockDataStore.getRecentSearches());
    }

    public void clearRecentSearches() {
        MockDataStore.clearRecentSearches();
        loadRecentSearches();
    }

    public void setFilterType(String type) {
        this.currentFilterType = type;
        // Re-trigger search with current data if needed (handled in Fragment)
    }

    public void onSearchQueryChanged(final String rawQuery) {
        final String query = normalize(rawQuery);
        cancelPendingSearch();

        if (query.isEmpty()) {
            resetToCategories();
            return;
        }

        searchRunnable = new Runnable() {
            @Override
            public void run() {
                MockDataStore.addRecentSearch(query); // Lưu lịch sử
                loadRecentSearches();
                performNameSearch(query);
            }
        };
        searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY_MS);
    }

    public void onCategorySelected(String categoryName) {
        final String category = normalize(categoryName);
        cancelPendingSearch();
        if (category.isEmpty()) {
            resetToCategories();
            return;
        }
        performCategorySearch(category);
    }

    private void performNameSearch(String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        List<SearchResultItem> results = new ArrayList<>();

        if (currentFilterType.equals("ALL") || currentFilterType.equals("ARTIST")) {
            for (Artist artist : MockDataStore.getArtists()) {
                if (matchesArtistName(artist, normalizedQuery)) results.add(mapArtist(artist));
            }
        }

        if (currentFilterType.equals("ALL") || currentFilterType.equals("ALBUM")) {
            for (Album album : MockDataStore.getAlbums()) {
                if (contains(album.getTitle(), normalizedQuery)) results.add(mapAlbum(album));
            }
        }

        if (currentFilterType.equals("ALL") || currentFilterType.equals("SONG")) {
            for (Song song : MockDataStore.getSongs()) {
                if (matchesSongName(song, normalizedQuery)) results.add(mapSong(song));
            }
        }

        searchResultsLiveData.setValue(results);
        showCategoriesLiveData.setValue(false);
    }

    private void performCategorySearch(String category) {
        String normalizedCategory = category.toLowerCase(Locale.ROOT);
        List<SearchResultItem> results = new ArrayList<>();

        if (currentFilterType.equals("ALL") || currentFilterType.equals("ARTIST")) {
            for (Artist artist : MockDataStore.getArtists()) {
                if (contains(artist.getGenre(), normalizedCategory)) results.add(mapArtist(artist));
            }
        }
        if (currentFilterType.equals("ALL") || currentFilterType.equals("SONG")) {
            for (Song song : MockDataStore.getSongs()) {
                if (contains(song.getGenre(), normalizedCategory)) results.add(mapSong(song));
            }
        }

        searchResultsLiveData.setValue(results);
        showCategoriesLiveData.setValue(false);
    }

    private boolean matchesSongName(Song song, String query) {
        return contains(song.getTitle(), query) || contains(song.getArtistName(), query);
    }

    private boolean matchesArtistName(Artist artist, String query) {
        return contains(artist.getName(), query);
    }

    private SearchResultItem mapSong(Song song) {
        return new SearchResultItem(song.getId(), SearchResultItem.TYPE_SONG, safe(song.getTitle()), buildSongSubtitle(song), safe(song.getCoverUrl()), safe(song.getArtistId()), safe(song.getAlbumId()));
    }

    private SearchResultItem mapArtist(Artist artist) {
        return new SearchResultItem(artist.getId(), SearchResultItem.TYPE_ARTIST, safe(artist.getName()), buildArtistSubtitle(artist), safe(artist.getImageUrl()), safe(artist.getId()), "");
    }

    private SearchResultItem mapAlbum(Album album) {
        return new SearchResultItem(album.getId(), SearchResultItem.TYPE_ALBUM, safe(album.getTitle()), "Album • " + safe(album.getArtistName()), safe(album.getCoverUrl()), safe(album.getArtistId()), safe(album.getId()));
    }

    private String buildSongSubtitle(Song song) {
        String artistName = safe(song.getArtistName());
        return !artistName.isEmpty() ? "Bài hát • " + artistName : "Bài hát";
    }

    private String buildArtistSubtitle(Artist artist) {
        return "Nghệ sĩ";
    }

    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalize(String value) { return value == null ? "" : value.trim(); }
    private String safe(String value) { return value == null ? "" : value; }

    private void resetToCategories() {
        searchResultsLiveData.setValue(new ArrayList<SearchResultItem>());
        showCategoriesLiveData.setValue(true);
    }

    private void cancelPendingSearch() {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
            searchRunnable = null;
        }
    }
}