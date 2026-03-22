package com.melodix.app.View.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.R;
import com.melodix.app.View.search.adapter.CategoryAdapter;
import com.melodix.app.View.search.adapter.SearchResultAdapter;
import com.melodix.app.ViewModel.SearchViewModel;
import com.melodix.app.View.album.AlbumDetailActivity;
import com.melodix.app.View.artist.ArtistDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText edtSearch;
    private RecyclerView rvSearchContent;
    private ChipGroup chipGroupFilter;
    private ChipGroup chipGroupRecent;
    private LinearLayout layoutRecentSearches;
    private TextView tvClearRecent;

    private SearchViewModel searchViewModel;
    private CategoryAdapter categoryAdapter;
    private SearchResultAdapter searchResultAdapter;
    private GridLayoutManager gridLayoutManager;
    private LinearLayoutManager linearLayoutManager;

    private TextWatcher searchTextWatcher;
    private boolean ignoreSearchTextCallback = false;

    public SearchFragment() {
        super(R.layout.fragment_search);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupFilters();
        setupViewModel();
        setupSearchInput();
        syncRestoredQueryState();
    }

    private void initViews(@NonNull View view) {
        edtSearch = view.findViewById(R.id.edtSearch);
        rvSearchContent = view.findViewById(R.id.rvSearchContent);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipGroupRecent = view.findViewById(R.id.chipGroupRecent);
        layoutRecentSearches = view.findViewById(R.id.layoutRecentSearches);
        tvClearRecent = view.findViewById(R.id.tvClearRecent);

        edtSearch.setOnClickListener(v -> {
            edtSearch.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(edtSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });

        tvClearRecent.setOnClickListener(v -> searchViewModel.clearRecentSearches());
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipSong) searchViewModel.setFilterType("SONG");
            else if (id == R.id.chipArtist) searchViewModel.setFilterType("ARTIST");
            else if (id == R.id.chipAlbum) searchViewModel.setFilterType("ALBUM");
            else searchViewModel.setFilterType("ALL");

            // Tìm kiếm lại với bộ lọc mới
            searchViewModel.onSearchQueryChanged(edtSearch.getText().toString());
        });
    }

    private void setupRecyclerView() {
        gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        linearLayoutManager = new LinearLayoutManager(requireContext());

        categoryAdapter = new CategoryAdapter(categoryName -> {
            ignoreSearchTextCallback = true;
            edtSearch.setText(categoryName);
            edtSearch.setSelection(categoryName.length());
            searchViewModel.onCategorySelected(categoryName);
        });

        searchResultAdapter = new SearchResultAdapter(this::handleSearchResultClick);

        rvSearchContent.setLayoutManager(gridLayoutManager);
        rvSearchContent.setAdapter(categoryAdapter);
        rvSearchContent.setHasFixedSize(true);
    }

    private void handleSearchResultClick(SearchResultItem item) {
        if (item == null) return;
        if (item.getType() == SearchResultItem.TYPE_ARTIST) {
            String artistId = TextUtils.isEmpty(item.getArtistId()) ? item.getId() : item.getArtistId();
            if (!TextUtils.isEmpty(artistId)) startActivity(ArtistDetailActivity.newIntent(requireContext(), artistId));
            return;
        }
        if (item.getType() == SearchResultItem.TYPE_ALBUM) {
            String albumId = item.getAlbumId();
            if (!TextUtils.isEmpty(albumId)) startActivity(AlbumDetailActivity.newIntent(requireContext(), albumId));
            return;
        }
        String artistIdFallback = item.getArtistId();
        if (!TextUtils.isEmpty(artistIdFallback)) {
            startActivity(ArtistDetailActivity.newIntent(requireContext(), artistIdFallback));
        }
    }

    private void setupViewModel() {
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        searchViewModel.getCategoriesLiveData().observe(getViewLifecycleOwner(), categories -> {
            categoryAdapter.submitList(categories == null ? new ArrayList<>() : new ArrayList<>(categories));
        });

        searchViewModel.getSearchResultsLiveData().observe(getViewLifecycleOwner(), searchResultItems -> {
            searchResultAdapter.submitList(searchResultItems == null ? new ArrayList<>() : new ArrayList<>(searchResultItems));
        });

        searchViewModel.getShowCategoriesLiveData().observe(getViewLifecycleOwner(), showCategories -> {
            if (Boolean.TRUE.equals(showCategories)) {
                renderCategoryMode();
            } else {
                renderResultMode();
            }
        });

        searchViewModel.getRecentSearchesLiveData().observe(getViewLifecycleOwner(), recents -> {
            chipGroupRecent.removeAllViews();
            if (recents == null || recents.isEmpty()) {
                layoutRecentSearches.setVisibility(View.GONE);
            } else {
                layoutRecentSearches.setVisibility(View.VISIBLE);
                for (String keyword : recents) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(keyword);
                    chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#294039")));                    chip.setTextColor(android.graphics.Color.WHITE);
                    chip.setOnClickListener(v -> {
                        edtSearch.setText(keyword);
                        edtSearch.setSelection(keyword.length());
                    });
                    chipGroupRecent.addView(chip);
                }
            }
        });
    }

    private void setupSearchInput() {
        searchTextWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (ignoreSearchTextCallback) {
                    ignoreSearchTextCallback = false;
                    return;
                }
                searchViewModel.onSearchQueryChanged(s.toString());
            }
        };
        edtSearch.addTextChangedListener(searchTextWatcher);
    }

    private void syncRestoredQueryState() {
        edtSearch.post(() -> {
            if (searchViewModel != null) searchViewModel.onSearchQueryChanged(edtSearch.getText().toString());
        });
    }

    private void renderCategoryMode() {
        if (rvSearchContent.getLayoutManager() != gridLayoutManager) rvSearchContent.setLayoutManager(gridLayoutManager);
        if (rvSearchContent.getAdapter() != categoryAdapter) rvSearchContent.setAdapter(categoryAdapter);
        rvSearchContent.scrollToPosition(0);
        layoutRecentSearches.setVisibility(chipGroupRecent.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void renderResultMode() {
        if (rvSearchContent.getLayoutManager() != linearLayoutManager) rvSearchContent.setLayoutManager(linearLayoutManager);
        if (rvSearchContent.getAdapter() != searchResultAdapter) rvSearchContent.setAdapter(searchResultAdapter);
        rvSearchContent.scrollToPosition(0);
        layoutRecentSearches.setVisibility(View.GONE); // Ẩn lịch sử khi đang tìm kiếm
    }
}