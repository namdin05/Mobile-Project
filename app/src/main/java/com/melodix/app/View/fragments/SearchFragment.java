package com.melodix.app.View.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.chip.Chip;
import com.melodix.app.Model.SearchResultItem;
import com.melodix.app.Model.Song;
import com.melodix.app.R;
import com.melodix.app.Repository.AppRepository;
import com.melodix.app.Utils.Constants;

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

        // Đã ẩn phần Genre (Thể loại) vì dữ liệu MockDatabase đã được rút gọn
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

        etSearch.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                runSearch();
                return true;
            }
            return false;
        });

        return view;
    }

    private void bindChips(View view) {
        ((Chip) view.findViewById(R.id.chip_all)).setOnClickListener(v -> { filter = Constants.FILTER_ALL; runSearch(); });
        ((Chip) view.findViewById(R.id.chip_song)).setOnClickListener(v -> { filter = Constants.FILTER_SONG; runSearch(); });
        ((Chip) view.findViewById(R.id.chip_artist)).setOnClickListener(v -> { filter = Constants.FILTER_ARTIST; runSearch(); });
        ((Chip) view.findViewById(R.id.chip_album)).setOnClickListener(v -> { filter = Constants.FILTER_ALBUM; runSearch(); });
        ((Chip) view.findViewById(R.id.chip_playlist)).setOnClickListener(v -> { filter = Constants.FILTER_PLAYLIST; runSearch(); });
    }

    private void renderRecentSearches() {
        recentContainer.removeAllViews();
        ArrayList<String> recent = repository.getRecentSearches();
        if (recent == null) return;
        for (String keyword : recent) {
            TextView chip = new TextView(requireContext());
            chip.setText(keyword);
            chip.setPadding(28, 16, 28, 16);
            chip.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.mdx_text));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.rightMargin = 18;
            chip.setLayoutParams(params);
            chip.setOnClickListener(v -> {
                etSearch.setText(keyword);
                etSearch.setSelection(keyword.length());
                runSearch();
            });
            recentContainer.addView(chip);
        }
    }

    private void runSearch() {
        etSearch.clearFocus();
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        }
        String keyword = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            showDefaultResults();
            tvRecentLabel.setVisibility(View.VISIBLE);
            recentScroll.setVisibility(View.VISIBLE);
            tvResultsLabel.setVisibility(View.GONE);
            rvResults.setVisibility(View.GONE);
            renderRecentSearches();
            return;
        }
        tvRecentLabel.setVisibility(View.GONE);
        recentScroll.setVisibility(View.GONE);
        tvResultsLabel.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.VISIBLE);

        // =======================================================
        // ĐÂY LÀ PHẦN LỘT XÁC: GỌI MẠNG BẤT ĐỒNG BỘ (ASYNC)
        // =======================================================
        repository.search(keyword, filter, new AppRepository.SearchCallback() {
            @Override
            public void onSuccess(ArrayList<SearchResultItem> results) {
                // Retrofit rất thông minh, nó tự động trả kết quả về Luồng chính (Main Thread)
                // Nên bạn cứ thoải mái cập nhật giao diện ở đây mà không sợ crash app!
                resultAdapter.update(results);
                renderRecentSearches();
            }

            @Override
            public void onError(String message) {
                // Hiển thị thông báo nhỏ ở dưới đáy màn hình nếu mạng chập chờn
                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDefaultResults() {
        ArrayList<SearchResultItem> defaults = new ArrayList<>();
        // Sửa từ getTopTrendingSongs() thành getAllApprovedSongs() để có data hiển thị
        ArrayList<Song> allSongs = repository.getAllApprovedSongs();

        if (allSongs != null) {
            for (Song song : allSongs) {
                defaults.add(new SearchResultItem(Constants.FILTER_SONG, song.id, song.title, song.artistName + " • " + song.genre, song.coverRes));
            }
        }
        resultAdapter.update(defaults);
    }

    private void openResult(SearchResultItem item) {
        switch (item.type) {
            case Constants.FILTER_SONG:
                // Tạm thời bỏ logic phát nhạc phức tạp, để lại khung sườn
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
}