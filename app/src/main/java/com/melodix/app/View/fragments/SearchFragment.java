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
    private LinearLayout recentContainer;
    private String filter = Constants.FILTER_ALL;
    private TextView tvRecentLabel;
    private View recentScroll;
    private TextView tvBrowseLabel;
    private androidx.recyclerview.widget.RecyclerView rvBrowse;
    private TextView tvResultsLabel;
    private androidx.recyclerview.widget.RecyclerView rvResults;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        repository = AppRepository.getInstance(requireContext());

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

        // 1. NÚT XÓA (CLEAR) TRÊN THANH TÌM KIẾM
        etSearch.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (etSearch.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                    // Nếu bấm vào khu vực của dấu X (bên phải)
                    if (event.getRawX() >= (etSearch.getRight() - etSearch.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - etSearch.getPaddingRight() - 20)) {
                        etSearch.setText(""); // Xóa nội dung
                        return true;
                    }
                }
            }
            return false;
        });

        // 2. SỰ KIỆN GÕ PHÍM (Live Search + Hiện dấu X)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Tự động hiện/ẩn dấu X ở thanh tìm kiếm
                if (s.length() > 0) {
                    etSearch.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0);
                } else {
                    etSearch.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }

                if (searchRunnable != null) {
                    debounceHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> runSearch();
                debounceHandler.postDelayed(searchRunnable, 500); // Debounce 500ms
            }
        });

        // 3. SỰ KIỆN LƯU LỊCH SỬ KHI BẤM ENTER
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

        // ==========================================
        // 4. HẠ BÀN PHÍM KHI TƯƠNG TÁC VỚI MÀN HÌNH
        // ==========================================

        // 4.1. Khi bắt đầu cuộn danh sách kết quả
        rvResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                }
            }
        });

       //  4.2. Khi chạm vào vùng của danh sách kết quả (nhưng không trúng item nào)
        rvResults.setOnTouchListener((v, event) -> {
            hideKeyboard();
            return false; // Giữ nguyên false để không chặn sự kiện click vào bài hát
        });

        setupUI(view);

        return view;
    }

    // 1. Thay thế hàm bindChips cũ bằng hàm này
    private void bindChips(View view) {
        Chip chipAll = view.findViewById(R.id.chip_all);
        Chip chipSong = view.findViewById(R.id.chip_song);
        Chip chipArtist = view.findViewById(R.id.chip_artist);
        Chip chipAlbum = view.findViewById(R.id.chip_album);
        Chip chipPlaylist = view.findViewById(R.id.chip_playlist);

        // Gắn sự kiện click: Khi bấm vào thì Cập nhật Filter -> Đổi màu UI -> Chạy tìm kiếm
        if (chipAll != null) chipAll.setOnClickListener(v -> { filter = Constants.FILTER_ALL; updateChipUI(); runSearch(); });
        if (chipSong != null) chipSong.setOnClickListener(v -> { filter = Constants.FILTER_SONG; updateChipUI(); runSearch(); });
        if (chipArtist != null) chipArtist.setOnClickListener(v -> { filter = Constants.FILTER_ARTIST; updateChipUI(); runSearch(); });
        if (chipAlbum != null) chipAlbum.setOnClickListener(v -> { filter = Constants.FILTER_ALBUM; updateChipUI(); runSearch(); });
        if (chipPlaylist != null) chipPlaylist.setOnClickListener(v -> { filter = Constants.FILTER_PLAYLIST; updateChipUI(); runSearch(); });

        // Gọi hàm này lần đầu tiên khi mở app để "bật sáng" thẻ mặc định (Tất cả)
        updateChipUI();
    }

    // 2. Thêm hàm mới này ngay bên dưới hàm bindChips
    // Nhiệm vụ: Xử lý hiệu ứng thị giác (Làm mờ 50% thẻ chưa chọn, Sáng 100% thẻ đang chọn)
    private void updateChipUI() {
        View view = getView();
        if (view == null) return;

        Chip chipAll = view.findViewById(R.id.chip_all);
        Chip chipSong = view.findViewById(R.id.chip_song);
        Chip chipArtist = view.findViewById(R.id.chip_artist);
        Chip chipAlbum = view.findViewById(R.id.chip_album);
        Chip chipPlaylist = view.findViewById(R.id.chip_playlist);

        // Đưa tất cả các thẻ về trạng thái "Ngủ" (Mờ đi một nửa để nhường "spotlight")
        Chip[] allChips = {chipAll, chipSong, chipArtist, chipAlbum, chipPlaylist};
        for (Chip c : allChips) {
            if (c != null) {
                c.setAlpha(0.4f); // Độ mờ 40% (Trông rất sang trọng và dịu mắt)
            }
        }

        // Bật sáng 100% (Alpha = 1.0f) cho duy nhất thẻ đang được kích hoạt
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
            // =======================================================
            // TẠO THẺ TÌM KIẾM CÓ CHỨA DẤU X BÊN TRONG
            // =======================================================
            LinearLayout chipLayout = new LinearLayout(requireContext());
            chipLayout.setOrientation(LinearLayout.HORIZONTAL);
            chipLayout.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge));
            chipLayout.setPadding(32, 16, 32, 16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = 18;
            params.bottomMargin = 16;
            chipLayout.setLayoutParams(params);

            // 1. Chữ từ khóa
            TextView tvKeyword = new TextView(requireContext());
            tvKeyword.setText(keyword);
            tvKeyword.setTextColor(ContextCompat.getColor(requireContext(), R.color.mdx_text));

            // 2. Chữ X
            TextView tvClose = new TextView(requireContext());
            tvClose.setText(" ✕"); // Dấu X đẹp
            tvClose.setTextColor(ContextCompat.getColor(requireContext(), R.color.mdx_text));
            tvClose.setPadding(16, 0, 0, 0);

            // Sự kiện: Bấm vào chữ để tìm kiếm lại
            tvKeyword.setOnClickListener(v -> {
                etSearch.setText(keyword);
                etSearch.setSelection(keyword.length()); // Đưa con trỏ về cuối
                hideKeyboard();
                repository.saveToRecentSearch(keyword);
                renderRecentSearches();
                runSearch();
            });

            // Sự kiện: Bấm vào dấu X để xóa lẻ từ khóa này
            tvClose.setOnClickListener(v -> {
                repository.removeRecentSearch(keyword); // Gọi hàm mới
                renderRecentSearches(); // Cập nhật lại list ngay lập tức
            });

            chipLayout.addView(tvKeyword);
            chipLayout.addView(tvClose);
            recentContainer.addView(chipLayout);
        }

        // Nút Xóa tất cả lịch sử


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

        repository.search(keyword, filter, new AppRepository.SearchCallback() {
            @Override
            public void onSuccess(ArrayList<SearchResultItem> results) {
                resultAdapter.update(results);
            }

            @Override
            public void onError(String message) {
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

    // Hàm hỗ trợ hạ bàn phím và bỏ con trỏ nhấp nháy ở ô Search
    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    // ==========================================
    // BÍ THUẬT HẠ BÀN PHÍM HOÀN HẢO CHO FRAGMENT
    // ==========================================
    public void setupUI(View view) {
        // Nếu cái view đang chạm vào KHÔNG PHẢI là ô gõ chữ (EditText)
        if (!(view instanceof EditText)) {
            view.setOnTouchListener((v, event) -> {
                hideKeyboard();
                return false;
            });
        }

        // Nếu cái view này là một cái hộp chứa (ViewGroup) nhiều phần tử khác
        // Dùng vòng lặp đệ quy để quét hết tất cả mọi ngóc ngách bên trong nó
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }
}