package com.melodix.app.View.music;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Repository.CommentRepository;
import com.melodix.app.Model.Comment;
import com.melodix.app.Model.SessionManager;
import com.melodix.app.View.adapters.CommentAdapter;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.melodix.app.R;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;



public class CommentsBottomSheet extends BottomSheetDialogFragment {

    private String songId;
    private TextView tvAiContent;
    private RecyclerView rvComments;
    private EditText edtComment;
    private ImageButton btnSend;
    private CommentRepository commentRepository;
    private CommentAdapter commentAdapter;
    private final List<Comment> commentList = new ArrayList<>();
    private static final String EDGE_FUNCTION_URL = "https://ggektdtrjagrmfnimmaw.supabase.co/functions/v1/summarize-comments";

    public static CommentsBottomSheet newInstance(String songId) {
        CommentsBottomSheet fragment = new CommentsBottomSheet();
        Bundle args = new Bundle();
        args.putString("SONG_ID", songId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            songId = getArguments().getString("SONG_ID");
        }
        commentRepository = new CommentRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_comments, container, false);

        // Ánh xạ UI
        tvAiContent = view.findViewById(R.id.tv_ai_summary_content);
        rvComments = view.findViewById(R.id.rv_comments);
        edtComment = view.findViewById(R.id.edt_comment_input);
        btnSend = view.findViewById(R.id.btn_send_comment);

        setupRecyclerView();
        // Vừa mở Bottom Sheet lên là gọi Edge Function lấy tóm tắt AI ngay
        fetchAiSummaryFromEdgeFunction();

        // TODO: Sau này bạn gọi API Supabase ở đây để lấy danh sách Comment thật đổ vào rv_comments
         fetchCommentsList();
        btnSend.setOnClickListener(v -> postNewComment());

        return view;
    }

    private void fetchAiSummaryFromEdgeFunction() {
        if (songId == null || songId.isEmpty()) {
            updateUi("Lỗi: Không tìm thấy bài hát.");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // Đóng gói song_id thành JSON để gửi cho Edge Function
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("song_id", songId);
        } catch (Exception ignored) {}

        RequestBody body = RequestBody.create(JSON, jsonBody.toString());

        Request request = new Request.Builder()
                .url(EDGE_FUNCTION_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                updateUi("Lỗi kết nối đến máy chủ. Vui lòng thử lại.");
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Nhận JSON từ Edge Function trả về
                        String jsonResponse = response.body().string();

                        android.util.Log.e("MELODIX_DEBUG", "Server Supabase trả về: " + jsonResponse);

                        JSONObject resJson = new JSONObject(jsonResponse);

                        if (resJson.has("summary")) {
                            String summary = resJson.getString("summary");
                            updateUi(summary);
                        } else if (resJson.has("error")) {
                            updateUi("Lỗi Backend: " + resJson.getString("error"));
                        } else {
                            updateUi("Không thể phân tích dữ liệu từ máy chủ.");
                        }
                    } catch (Exception e) {
                        updateUi("Lỗi đọc dữ liệu máy chủ.");
                        e.printStackTrace();
                    }
                } else {
                    updateUi("Có lỗi xảy ra khi lấy tóm tắt AI (Code: " + response.code() + ").");
                }
            }
        });
    }

    private void setupRecyclerView() {
        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        commentAdapter = new CommentAdapter(requireContext(), commentList);
        rvComments.setAdapter(commentAdapter);
    }

    private void fetchCommentsList() {
        if (songId == null) return;

        commentRepository.getCommentsBySong(songId, new retrofit2.Callback<List<Comment>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Comment>> call, retrofit2.Response<List<Comment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    commentList.clear();
                    commentList.addAll(response.body());
                    if (commentAdapter != null) {
                        commentAdapter.notifyDataSetChanged();
                    }
                    Log.d("COMMENTS", "Loaded " + commentList.size() + " comments");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<List<Comment>> call, Throwable t) {
                Log.e("COMMENTS", "Failed to load comments: " + t.getMessage());
                showToast("Không tải được bình luận");
            }
        });
    }

    private void postNewComment() {
        String content = edtComment.getText().toString().trim();
        if (content.isEmpty()) return;

        SessionManager session = SessionManager.getInstance(requireContext());
        if (session.getCurrentUser() == null) {
            showToast("Vui lòng đăng nhập để bình luận");
            return;
        }

        // Log để debug
        Log.d("COMMENT_DEBUG", "Posting comment - SongId: " + songId + ", UserId: " + session.getCurrentUser().getId() + ", Content: " + content);

        commentRepository.postComment(
                songId,
                session.getCurrentUser().getId(),
                content,
                new retrofit2.Callback<ResponseBody>() {
                    @Override
                    public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            edtComment.setText("");
                            showToast("Bình luận đã được đăng!");
                            fetchCommentsList();
                        } else {
                            // Log lỗi chi tiết từ server
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                                Log.e("COMMENT_ERROR", "Error code: " + response.code() + ", Error body: " + errorBody);
                                showToast("Đăng bình luận thất bại: " + response.code());
                            } catch (IOException e) {
                                e.printStackTrace();
                                showToast("Đăng bình luận thất bại");
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                        Log.e("COMMENT_ERROR", "Network error: " + t.getMessage());
                        showToast("Lỗi kết nối: " + t.getMessage());
                    }
                });
    }


    // Hàm phụ trợ giúp an toàn cập nhật UI từ Background Thread của OkHttp
    private void updateUi(String text) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (tvAiContent != null) {
                tvAiContent.setText(text);
            }
        });
    }
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        );
    }
}