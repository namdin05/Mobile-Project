package com.melodix.app.View.admin; // Sửa lại đường dẫn package nếu của bạn khác

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.melodix.app.Adapter.AdminGenreAdapter;
import com.melodix.app.BuildConfig;
import com.melodix.app.Constants;
import com.melodix.app.Model.Genre;
import com.melodix.app.R;
import com.melodix.app.Service.AdminAPIService;
import com.melodix.app.Service.ProfileAPIService;
import com.melodix.app.Service.RetrofitClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminGenreFragment extends Fragment {

    private RecyclerView rvGenres;
    private MaterialButton btnAddGenre;

    private AdminGenreAdapter adapter;
    private List<Genre> genreList;

    // Biến dùng cho việc chọn và upload ảnh
    private Uri selectedImageUri = null;
    private ImageView currentDialogImageView = null;

    // Máy quét thư viện ảnh
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    // Load ảnh tạm lên giao diện Dialog
                    if (currentDialogImageView != null) {
                        Glide.with(requireContext()).load(selectedImageUri).into(currentDialogImageView);
                    }
                }
            }
    );

    public AdminGenreFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_genre, container, false);

        // Ánh xạ View
        rvGenres = view.findViewById(R.id.rvGenres);
        btnAddGenre = view.findViewById(R.id.btnAddGenre);

        // Setup RecyclerView
        rvGenres.setLayoutManager(new LinearLayoutManager(getContext()));
        genreList = new ArrayList<>();
        adapter = new AdminGenreAdapter(getContext(), genreList);
        rvGenres.setAdapter(adapter);

        // Kéo dữ liệu khi mở màn hình
        fetchGenres();

        // Nút thêm mới
        btnAddGenre.setOnClickListener(v -> showAddEditDialog(null));

        return view;
    }

    // ==========================================
    // 1. HÀM KÉO DANH SÁCH THỂ LOẠI
    // ==========================================
    private void fetchGenres() {
        AdminAPIService apiService = RetrofitClient.getClient().create(AdminAPIService.class);
        String apiKey = BuildConfig.SERVICE_KEY;
        String token = "Bearer " + BuildConfig.SERVICE_KEY;

        apiService.getAllGenres(apiKey, token).enqueue(new Callback<List<Genre>>() {
            @Override
            public void onResponse(Call<List<Genre>> call, Response<List<Genre>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    genreList.clear();
                    // Lọc: Chỉ hiển thị những thể loại có is_visible = true (chưa bị xóa mềm)
                    for (Genre genre : response.body()) {
                        if (genre.isVisible()) {
                            genreList.add(genre);
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Genre>> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // 2. HÀM MỞ CỬA SỔ THÊM / SỬA (DIALOG)
    // ==========================================
    public void showAddEditDialog(Genre genre) {
        selectedImageUri = null; // Reset ảnh mỗi lần mở

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_edit_genre, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Làm nền trong suốt để viền bo tròn của XML hoạt động
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextInputEditText edtName = view.findViewById(R.id.edtGenreName);
        ImageView imgPreview = view.findViewById(R.id.imgGenrePreview);
        MaterialButton btnPickImage = view.findViewById(R.id.btnPickImage);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveGenre);

        currentDialogImageView = imgPreview; // Gán cho biến toàn cục để máy quét ảnh dùng

        boolean isEdit = (genre != null);
        if (isEdit) {
            tvTitle.setText("Edit Genre");
            edtName.setText(genre.getName());
            if (genre.getCoverUrl() != null && !genre.getCoverUrl().isEmpty()) {
                Glide.with(requireContext()).load(genre.getCoverUrl()).into(imgPreview);
            }
        }

        // Bấm nút chọn ảnh
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Bấm nút Lưu
        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            if (name.isEmpty()) {
                edtName.setError("Tên không được để trống");
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            if (selectedImageUri != null) {
                uploadGenreImage(name, isEdit ? genre.getId() : null, dialog);
            } else {
                saveGenreToDatabase(name, isEdit ? genre.getCoverUrl() : "", isEdit ? genre.getId() : null, dialog);
            }
        });

        dialog.show();
    }

    // ==========================================
    // 3. HÀM UPLOAD ẢNH LÊN STORAGE
    // ==========================================
    private void uploadGenreImage(String genreName, String genreId, AlertDialog dialog) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageBytes = byteBuffer.toByteArray();
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);

            String fileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + ".jpg";

            ProfileAPIService apiService = RetrofitClient.getClient().create(ProfileAPIService.class);

            apiService.uploadFileToStorage(
                    BuildConfig.SERVICE_KEY,
                    "Bearer " + BuildConfig.SERVICE_KEY,
                    "image/jpeg",
                    "true",
                    Constants.GENRE_COVER_BUCKET.replace("/", ""),
                    fileName,
                    requestBody
            ).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String newImageUrl = Constants.STORAGE_BASE_URL + Constants.GENRE_COVER_BUCKET + fileName;
                        saveGenreToDatabase(genreName, newImageUrl, genreId, dialog);
                    } else {
                        Toast.makeText(getContext(), "Lỗi tải ảnh lên Storage", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi xử lý file ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================
    // 4. HÀM LƯU VÀO DATABASE (CREATE / UPDATE)
    // ==========================================
    private void saveGenreToDatabase(String name, String coverUrl, String genreId, AlertDialog dialog) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("cover_url", coverUrl);
        data.put("is_visible", true);

        AdminAPIService apiService = RetrofitClient.getClient().create(AdminAPIService.class);
        String key = BuildConfig.SERVICE_KEY;
        String token = "Bearer " + key;

        if (genreId != null) {
            apiService.updateGenre(key, token, "eq." + genreId, data).enqueue(createCallback(dialog));
        } else {
            apiService.createGenre(key, token, data).enqueue(createCallback(dialog));
        }
    }

    // ==========================================
    // 5. HÀM XÓA MỀM (SOFT DELETE)
    // ==========================================
    public void softDeleteGenre(Genre genre) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn ẩn thể loại " + genre.getName() + "? Các bài hát liên quan vẫn an toàn.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("is_visible", false);

                    AdminAPIService apiService = RetrofitClient.getClient().create(AdminAPIService.class);
                    apiService.updateGenre(BuildConfig.SERVICE_KEY, "Bearer " + BuildConfig.SERVICE_KEY, "eq." + genre.getId(), data)
                            .enqueue(createCallback(null));
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ==========================================
    // CALLBACK CHUNG CHO CÁC HÀM GỌI API
    // ==========================================
    private Callback<ResponseBody> createCallback(AlertDialog dialog) {
        return new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    if (dialog != null) dialog.dismiss();
                    fetchGenres();
                    Toast.makeText(getContext(), "Thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    // THÊM ĐOẠN NÀY VÀO ĐỂ BẮT LỖI SUPABASE
                    try {
                        String errorBody = response.errorBody().string();
                        android.util.Log.e("MELODIX_LỖI_DB", "Supabase chửi: " + errorBody);
                        Toast.makeText(getContext(), "Lỗi Database! Xem Logcat (chữ màu đỏ)", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        };
    }
}