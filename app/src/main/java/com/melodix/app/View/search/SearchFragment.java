package com.melodix.app.View.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.R;
import com.melodix.app.View.search.adapter.CategoryAdapter;
import com.melodix.app.View.search.adapter.SearchResultAdapter;
import com.melodix.app.ViewModel.SearchViewModel;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText edtSearch;
    private RecyclerView rvSearchContent;

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
        setupViewModel();
        setupSearchInput();
        syncRestoredQueryState();
    }

    private void initViews(@NonNull View view) {
        edtSearch = view.findViewById(R.id.edtSearch);
        rvSearchContent = view.findViewById(R.id.rvSearchContent);
    }

    private void setupRecyclerView() {
        gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        linearLayoutManager = new LinearLayoutManager(requireContext());

        categoryAdapter = new CategoryAdapter(new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(String categoryName) {
                ignoreSearchTextCallback = true;
                edtSearch.setText(categoryName);
                edtSearch.setSelection(categoryName.length());
                searchViewModel.onCategorySelected(categoryName);
            }
        });

        searchResultAdapter = new SearchResultAdapter(new SearchResultAdapter.OnSearchResultClickListener() {
            @Override
            public void onSearchResultClick(SearchResultItem item) {
                // TODO: bước tiếp theo có thể navigate sang Song Detail / Artist Detail
            }
        });

        rvSearchContent.setLayoutManager(gridLayoutManager);
        rvSearchContent.setAdapter(categoryAdapter);
        rvSearchContent.setHasFixedSize(true);
    }

    private void setupViewModel() {
        searchViewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        searchViewModel.getCategoriesLiveData().observe(getViewLifecycleOwner(),
                new Observer<List<String>>() {
                    @Override
                    public void onChanged(List<String> categories) {
                        if (categories == null) {
                            categoryAdapter.submitList(new ArrayList<String>());
                            return;
                        }
                        categoryAdapter.submitList(new ArrayList<>(categories));
                    }
                });

        searchViewModel.getSearchResultsLiveData().observe(getViewLifecycleOwner(),
                new Observer<List<SearchResultItem>>() {
                    @Override
                    public void onChanged(List<SearchResultItem> searchResultItems) {
                        if (searchResultItems == null) {
                            searchResultAdapter.submitList(new ArrayList<SearchResultItem>());
                            return;
                        }
                        searchResultAdapter.submitList(new ArrayList<>(searchResultItems));
                    }
                });

        searchViewModel.getShowCategoriesLiveData().observe(getViewLifecycleOwner(),
                new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean showCategories) {
                        if (Boolean.TRUE.equals(showCategories)) {
                            renderCategoryMode();
                        } else {
                            renderResultMode();
                        }
                    }
                });
    }

    private void setupSearchInput() {
        searchTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
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
        edtSearch.post(new Runnable() {
            @Override
            public void run() {
                if (searchViewModel != null) {
                    searchViewModel.onSearchQueryChanged(edtSearch.getText().toString());
                }
            }
        });
    }

    private void renderCategoryMode() {
        if (rvSearchContent.getLayoutManager() != gridLayoutManager) {
            rvSearchContent.setLayoutManager(gridLayoutManager);
        }
        if (rvSearchContent.getAdapter() != categoryAdapter) {
            rvSearchContent.setAdapter(categoryAdapter);
        }
        rvSearchContent.scrollToPosition(0);
    }

    private void renderResultMode() {
        if (rvSearchContent.getLayoutManager() != linearLayoutManager) {
            rvSearchContent.setLayoutManager(linearLayoutManager);
        }
        if (rvSearchContent.getAdapter() != searchResultAdapter) {
            rvSearchContent.setAdapter(searchResultAdapter);
        }
        rvSearchContent.scrollToPosition(0);
    }

    @Override
    public void onDestroyView() {
        if (edtSearch != null && searchTextWatcher != null) {
            edtSearch.removeTextChangedListener(searchTextWatcher);
        }

        if (rvSearchContent != null) {
            rvSearchContent.setAdapter(null);
        }

        searchTextWatcher = null;
        edtSearch = null;
        rvSearchContent = null;
        super.onDestroyView();
    }
}