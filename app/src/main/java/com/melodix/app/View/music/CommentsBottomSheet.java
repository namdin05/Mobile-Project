package com.melodix.app.View.music; // Thay đổi package cho khớp với project của bạn

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

public class CommentsBottomSheet extends BottomSheetDialogFragment {

    private String songId;
    private TextView tvAiContent;
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet_comments, container, false);

        // Ánh xạ UI
        tvAiContent = view.findViewById(R.id.tv_ai_summary_content);

        // Vừa mở Bottom Sheet lên là gọi Edge Function lấy tóm tắt AI ngay
        fetchAiSummaryFromEdgeFunction();

        // TODO: Sau này bạn gọi API Supabase ở đây để lấy danh sách Comment thật đổ vào rv_comments
        // fetchCommentsList();

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

    // Hàm phụ trợ giúp an toàn cập nhật UI từ Background Thread của OkHttp
    private void updateUi(String text) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (tvAiContent != null) {
                tvAiContent.setText(text);
            }
        });
    }
}