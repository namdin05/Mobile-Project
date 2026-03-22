package com.melodix.app.ViewModel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.melodix.app.Data.MockDataStore;
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

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    public SearchViewModel() {
        loadInitialState();
    }

    public LiveData<List<String>> getCategoriesLiveData() {
        return categoriesLiveData;
    }

    public LiveData<List<SearchResultItem>> getSearchResultsLiveData() {
        return searchResultsLiveData;
    }

    public LiveData<Boolean> getShowCategoriesLiveData() {
        return showCategoriesLiveData;
    }

    private void loadInitialState() {
        categoriesLiveData.setValue(MockDataStore.getGenres());
        searchResultsLiveData.setValue(new ArrayList<SearchResultItem>());
        showCategoriesLiveData.setValue(true);
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

        for (Artist artist : MockDataStore.getArtists()) {
            if (matchesArtistName(artist, normalizedQuery)) {
                results.add(mapArtist(artist));
            }
        }

        for (Song song : MockDataStore.getSongs()) {
            if (matchesSongName(song, normalizedQuery)) {
                results.add(mapSong(song));
            }
        }

        searchResultsLiveData.setValue(results);
        showCategoriesLiveData.setValue(false);
    }

    private void performCategorySearch(String category) {
        String normalizedCategory = category.toLowerCase(Locale.ROOT);
        List<SearchResultItem> results = new ArrayList<>();

        for (Artist artist : MockDataStore.getArtists()) {
            if (contains(artist.getGenre(), normalizedCategory)) {
                results.add(mapArtist(artist));
            }
        }

        for (Song song : MockDataStore.getSongs()) {
            if (contains(song.getGenre(), normalizedCategory)) {
                results.add(mapSong(song));
            }
        }

        searchResultsLiveData.setValue(results);
        showCategoriesLiveData.setValue(false);
    }

    private boolean matchesSongName(Song song, String query) {
        return contains(song.getTitle(), query)
                || contains(song.getArtistName(), query);
    }

    private boolean matchesArtistName(Artist artist, String query) {
        return contains(artist.getName(), query);
    }

    private SearchResultItem mapSong(Song song) {
        return new SearchResultItem(
                song.getId(),
                SearchResultItem.TYPE_SONG,
                safe(song.getTitle()),
                buildSongSubtitle(song),
                safe(song.getCoverUrl())
        );
    }

    private SearchResultItem mapArtist(Artist artist) {
        return new SearchResultItem(
                artist.getId(),
                SearchResultItem.TYPE_ARTIST,
                safe(artist.getName()),
                buildArtistSubtitle(artist),
                safe(artist.getImageUrl())
        );
    }

    private String buildSongSubtitle(Song song) {
        String artistName = safe(song.getArtistName());
        String albumTitle = safe(song.getAlbumTitle());

        if (!artistName.isEmpty() && !albumTitle.isEmpty()) {
            return "Bài hát • " + artistName + " • " + albumTitle;
        }

        if (!artistName.isEmpty()) {
            return "Bài hát • " + artistName;
        }

        return "Bài hát";
    }

    private String buildArtistSubtitle(Artist artist) {
        String genre = safe(artist.getGenre());

        if (!genre.isEmpty()) {
            return "Nghệ sĩ • " + genre;
        }

        return "Nghệ sĩ";
    }

    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

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

    @Override
    protected void onCleared() {
        super.onCleared();
        searchHandler.removeCallbacksAndMessages(null);
    }
}