package com.melodix.app.View.music;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.app.Repository.CommentRepository;
import com.melodix.app.Model.Comment;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.View.adapters.CommentAdapter;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.melodix.app.R;
import com.melodix.app.ViewModel.CommentViewModel;

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

    private CommentViewModel viewModel;

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
        return inflater.inflate(R.layout.layout_bottom_sheet_comments, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAiContent = view.findViewById(R.id.tv_ai_summary_content);
        rvComments = view.findViewById(R.id.rv_comments);
        edtComment = view.findViewById(R.id.edt_comment_input);
        btnSend = view.findViewById(R.id.btn_send_comment);

        viewModel = new ViewModelProvider(this).get(CommentViewModel.class);

        setupRecyclerView();
        fetchAiSummaryFromEdgeFunction();
        fetchCommentsList();

        btnSend.setOnClickListener(v -> postNewComment());

        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess != null && isSuccess) {
                edtComment.setText("");
                fetchCommentsList(); // Tải lại danh sách
            }
        });

        viewModel.getActionMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showToast(message);
            }
        });

        viewModel.getCommentsList().observe(getViewLifecycleOwner(), comments -> {
            if (comments != null) {
                commentList.clear();
                commentList.addAll(comments);
                if (commentAdapter != null) {
                    commentAdapter.notifyDataSetChanged();
                }
                Log.d("COMMENTS", "Loaded " + commentList.size() + " comments");
            }
        });

        // Lắng nghe lỗi nếu tải xịt
        viewModel.getFetchMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                showToast(message);
                Log.e("COMMENTS", message);
            }
        });
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
        // Giao toàn bộ việc gọi mạng cho ViewModel lo!
        viewModel.fetchComments(songId);
    }

    private void postNewComment() {
        String content = edtComment.getText().toString().trim();
        if (content.isEmpty()) return;

        // Quăng xuống cho ViewModel lo hết!
        viewModel.postNewComment(songId, content);
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