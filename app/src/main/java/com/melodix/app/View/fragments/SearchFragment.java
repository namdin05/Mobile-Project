package com.melodix.app.View.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.Constants;

import com.melodix.app.Utils.LoadingDialog;
import com.melodix.app.Utils.PlaybackUtils;
import com.melodix.app.View.AlbumDetailActivity;
import com.melodix.app.View.ArtistDetailActivity;
import com.melodix.app.View.PlaylistDetailActivity;
import com.melodix.app.View.adapters.SearchResultAdapter;
import java.util.ArrayList;

public class SearchFragment extends Fragment {
    private AppRepository repository;
    private SearchResultAdapter resultAdapter;
    private EditText etSearch;

    private android.widget.ListPopupWindow recentSearchPopup;
    private LinearLayout recentContainer;
    private String filter = Constants.FILTER_ALL;
    private TextView tvRecentLabel;
    private View recentScroll;
    private TextView tvBrowseLabel;
    private androidx.recyclerview.widget.RecyclerView rvBrowse;
    private TextView tvResultsLabel;
    private androidx.recyclerview.widget.RecyclerView rvResults;
    private LoadingDialog loadingDialog;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        repository = AppRepository.getInstance(requireContext());
        loadingDialog = new LoadingDialog();

        tvRecentLabel = view.findViewById(R.id.tv_recent_label);
        recentScroll = view.findViewById(R.id.recent_scroll);
        tvBrowseLabel = view.findViewById(R.id.tv_browse_label);
        etSearch = view.findViewById(R.id.et_search);
        recentContainer = view.findViewById(R.id.recent_search_container);

        rvBrowse = view.findViewById(R.id.rv_browse_genres);
        rvBrowse.setVisibility(View.GONE);
        tvBrowseLabel.setVisibility(View.GONE);

        tvResultsLabel = view.findViewById(R.id.tv_results_label);
        rvResults = view.findViewById(R.id.rv_results);
        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultAdapter = new SearchResultAdapter(requireContext(), new ArrayList<>(), this::openResult);
        rvResults.setAdapter(resultAdapter);

        tvResultsLabel.setVisibility(View.GONE);
        rvResults.setVisibility(View.GONE);

        bindChips(view);
        renderRecentSearches();

        
        etSearch.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etSearch.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    
                    if (event.getRawX() >= (etSearch.getRight() - etSearch.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - etSearch.getPaddingRight() - 20)) {
                        etSearch.setText(""); 
                        return true;
                    }
                }
            }
            return false;
        });
        etSearch.setOnClickListener(v -> {
            if (etSearch.getText().toString().trim().isEmpty()) {
                showRecentSearchDropdown();
            }
        });

        
        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etSearch.getText().toString().trim().isEmpty()) {
                showRecentSearchDropdown();
            }
        });
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                
                if (s.length() > 0) {
                    etSearch.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0);
                    if (recentSearchPopup != null && recentSearchPopup.isShowing()) {
                        recentSearchPopup.dismiss();
                    }
                } else {
                    etSearch.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    if (etSearch.hasFocus()) {
                        showRecentSearchDropdown();
                    }
                }

                if (searchRunnable != null) {
                    debounceHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> runSearch();
                debounceHandler.postDelayed(searchRunnable, 500); 
            }
        });

        
        etSearch.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String keyword = etSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(keyword)) {
                    repository.saveToRecentSearch(keyword);
                    renderRecentSearches();
                }
                hideKeyboard();
                runSearch();
                return true;
            }
            return false;
        });



        rvResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                }
            }
        });

        rvResults.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false;
        });

        setupUI(view);

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        renderRecentSearches();
    }

    private void showRecentSearchDropdown() {
        ArrayList<String> recent = repository.getRecentSearches();
        if (recent == null || recent.isEmpty()) return;

        
        java.util.List<String> top10 = new java.util.ArrayList<>(recent.subList(0, Math.min(recent.size(), 10)));

        if (recentSearchPopup == null) {
            recentSearchPopup = new android.widget.ListPopupWindow(requireContext());
            recentSearchPopup.setAnchorView(etSearch);

            
            etSearch.post(() -> recentSearchPopup.setWidth(etSearch.getWidth()));

            
            recentSearchPopup.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_card));
            recentSearchPopup.setVerticalOffset(10); 
        }

        
        android.widget.BaseAdapter customAdapter = new android.widget.BaseAdapter() {
            @Override
            public int getCount() { return top10.size(); }
            @Override
            public Object getItem(int position) { return top10.get(position); }
            @Override
            public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout layout;
                TextView tvKeyword;
                TextView tvClose;

                if (convertView == null) {
                    
                    layout = new LinearLayout(requireContext());
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.setPadding(40, 36, 40, 36); 
                    layout.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    
                    android.util.TypedValue outValue = new android.util.TypedValue();
                    requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                    layout.setBackgroundResource(outValue.resourceId);

                    
                    tvKeyword = new TextView(requireContext());
                    tvKeyword.setTextColor(ContextCompat.getColor(requireContext(), R.color.mdx_text));
                    tvKeyword.setTextSize(16f);
                    tvKeyword.setSingleLine(true);
                    tvKeyword.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    tvKeyword.setLayoutParams(params); 

                    
                    tvClose = new TextView(requireContext());
                    tvClose.setText("✕");
                    tvClose.setTextColor(ContextCompat.getColor(requireContext(), R.color.mdx_text_secondary)); 
                    tvClose.setTextSize(18f);
                    tvClose.setPadding(30, 10, 10, 10); 

                    
                    layout.addView(tvKeyword);
                    layout.addView(tvClose);
                } else {
                    
                    layout = (LinearLayout) convertView;
                    tvKeyword = (TextView) layout.getChildAt(0);
                    tvClose = (TextView) layout.getChildAt(1);
                }

                String keyword = top10.get(position);
                tvKeyword.setText(keyword);

                layout.setOnClickListener(v -> {
                    etSearch.setText(keyword);
                    etSearch.setSelection(keyword.length());
                    recentSearchPopup.dismiss(); 
                    hideKeyboard();
                    repository.saveToRecentSearch(keyword); 
                    renderRecentSearches();
                    runSearch();
                });

                tvClose.setOnClickListener(v -> {
                    repository.removeRecentSearch(keyword); 
                    top10.remove(position); 
                    notifyDataSetChanged(); 
                    renderRecentSearches(); 

                    
                    if (top10.isEmpty() && recentSearchPopup != null) {
                        recentSearchPopup.dismiss();
                    }
                });

                return layout;
            }
        };

        recentSearchPopup.setAdapter(customAdapter);
        recentSearchPopup.show();
    }
    
    private void bindChips(View view) {
        Chip chipAll = view.findViewById(R.id.chip_all);
        Chip chipSong = view.findViewById(R.id.chip_song);
        Chip chipArtist = view.findViewById(R.id.chip_artist);
        Chip chipAlbum = view.findViewById(R.id.chip_album);
        Chip chipPlaylist = view.findViewById(R.id.chip_playlist);

        
        if (chipAll != null) chipAll.setOnClickListener(v -> { filter = Constants.FILTER_ALL; updateChipUI(); runSearch(); });
        if (chipSong != null) chipSong.setOnClickListener(v -> { filter = Constants.FILTER_SONG; updateChipUI(); runSearch(); });
        if (chipArtist != null) chipArtist.setOnClickListener(v -> { filter = Constants.FILTER_ARTIST; updateChipUI(); runSearch(); });
        if (chipAlbum != null) chipAlbum.setOnClickListener(v -> { filter = Constants.FILTER_ALBUM; updateChipUI(); runSearch(); });
        if (chipPlaylist != null) chipPlaylist.setOnClickListener(v -> { filter = Constants.FILTER_PLAYLIST; updateChipUI(); runSearch(); });

        
        updateChipUI();
    }

    
    
    private void updateChipUI() {
        View view = getView();
        if (view == null) return;

        Chip chipAll = view.findViewById(R.id.chip_all);
        Chip chipSong = view.findViewById(R.id.chip_song);
        Chip chipArtist = view.findViewById(R.id.chip_artist);
        Chip chipAlbum = view.findViewById(R.id.chip_album);
        Chip chipPlaylist = view.findViewById(R.id.chip_playlist);

        
        Chip[] allChips = {chipAll, chipSong, chipArtist, chipAlbum, chipPlaylist};
        for (Chip c : allChips) {
            if (c != null) {
                c.setAlpha(0.4f); 
            }
        }

        
        if (Constants.FILTER_ALL.equals(filter) && chipAll != null) chipAll.setAlpha(1.0f);
        if (Constants.FILTER_SONG.equals(filter) && chipSong != null) chipSong.setAlpha(1.0f);
        if (Constants.FILTER_ARTIST.equals(filter) && chipArtist != null) chipArtist.setAlpha(1.0f);
        if (Constants.FILTER_ALBUM.equals(filter) && chipAlbum != null) chipAlbum.setAlpha(1.0f);
        if (Constants.FILTER_PLAYLIST.equals(filter) && chipPlaylist != null) chipPlaylist.setAlpha(1.0f);
    }

    private void renderRecentSearches() {
        recentContainer.removeAllViews();
        ArrayList<String> recent = repository.getRecentSearches();

        if (recent == null || recent.isEmpty()) return;

        for (String keyword : recent) {

            LinearLayout chipLayout = new LinearLayout(requireContext());
            chipLayout.setOrientation(LinearLayout.HORIZONTAL);
            chipLayout.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge));
            chipLayout.setPadding(32, 16, 32, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = 18;
            params.bottomMargin = 16;
            chipLayout.setLayoutParams(params);

            TextView tvKeyword = new TextView(requireContext());
            tvKeyword.setText(keyword);
            tvKeyword.setTextColor(ContextCompat.getColor(requireContext(), R.color.mdx_text));

            TextView tvClose = new TextView(requireContext());
            tvClose.setText(" ✕"); 
            tvClose.setTextColor(ContextCompat.getColor(requireContext(), R.color.mdx_text));
            tvClose.setPadding(16, 0, 0, 0);

            tvKeyword.setOnClickListener(v -> {
                etSearch.setText(keyword);
                etSearch.setSelection(keyword.length()); 
                hideKeyboard();
                repository.saveToRecentSearch(keyword);
                renderRecentSearches();
                runSearch();
            });

            tvClose.setOnClickListener(v -> {
                repository.removeRecentSearch(keyword); 
                renderRecentSearches(); 
            });

            chipLayout.addView(tvKeyword);
            chipLayout.addView(tvClose);
            recentContainer.addView(chipLayout);
        }
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = 20;

    }

    private void runSearch() {
        String keyword = etSearch.getText().toString().trim();

        if (TextUtils.isEmpty(keyword)) {
            showDefaultResults();
            tvRecentLabel.setVisibility(View.VISIBLE);
            recentScroll.setVisibility(View.VISIBLE);
            tvResultsLabel.setVisibility(View.GONE);
            rvResults.setVisibility(View.GONE);
            return;
        }

        tvRecentLabel.setVisibility(View.GONE);
        recentScroll.setVisibility(View.GONE);
        tvResultsLabel.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.VISIBLE);
        
        loadingDialog.showLoading(requireActivity());

        repository.search(keyword, filter, new AppRepository.SearchCallback() {
            @Override
            public void onSuccess(ArrayList<SearchResultItem> results) {
                loadingDialog.hideLoading();
                resultAdapter.update(results);
            }

            @Override
            public void onError(String message) {
                loadingDialog.hideLoading();
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDefaultResults() {
        ArrayList<SearchResultItem> defaults = new ArrayList<>();
        ArrayList<Song> allSongs = repository.getAllApprovedSongs();

        if (allSongs != null) {
            for (Song song : allSongs) {
                defaults.add(new SearchResultItem(Constants.FILTER_SONG, song.getId(), song.getTitle(), song.getArtistName() + " • " + song.getGenre(), song.getCoverUrl()));
            }
        }
        resultAdapter.update(defaults);
    }

    private void openResult(SearchResultItem item) {
        String historyKeyword = null;

        if (item != null && !TextUtils.isEmpty(item.title)) {
            historyKeyword = item.title.trim();   
        }

        if (TextUtils.isEmpty(historyKeyword)) {
            historyKeyword = etSearch.getText().toString().trim(); 
        }

        if (!TextUtils.isEmpty(historyKeyword)) {
            repository.saveToRecentSearch(historyKeyword);
            etSearch.setText(historyKeyword);
            etSearch.setSelection(historyKeyword.length());
            renderRecentSearches();
        }

        hideKeyboard();

        switch (item.type) {
            case Constants.FILTER_SONG:
                Song clickedSong = repository.getSongById(item.targetId);
                if (clickedSong != null) {
                    ArrayList<Song> playList = new ArrayList<>();
                    playList.add(clickedSong);
                    PlaybackUtils.playSong(requireContext(), playList, clickedSong.getId());
                }
                break;

            case Constants.FILTER_ARTIST:
                Intent artistIntent = new Intent(requireContext(), ArtistDetailActivity.class);
                artistIntent.putExtra(ArtistDetailActivity.EXTRA_ARTIST_ID, item.targetId);
                startActivity(artistIntent);
                break;

            case Constants.FILTER_ALBUM:
                Intent albumIntent = new Intent(requireContext(), AlbumDetailActivity.class);
                albumIntent.putExtra(AlbumDetailActivity.EXTRA_ALBUM_ID, item.targetId);
                startActivity(albumIntent);
                break;

            case Constants.FILTER_PLAYLIST:
                Intent playlistIntent = new Intent(requireContext(), PlaylistDetailActivity.class);
                playlistIntent.putExtra(PlaylistDetailActivity.EXTRA_PLAYLIST_ID, item.targetId);
                startActivity(playlistIntent);
                break;
        }
    }
    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    public void setupUI(View view) {
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                hideKeyboard();
                return false;
            });
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }
}