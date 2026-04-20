package com.melodix.app.View.admin.dashboard;

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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.melodix.app.Adapter.AdminGenreAdapter;
import com.melodix.app.Model.Genre;
import com.melodix.app.R;
import com.melodix.app.ViewModel.GenreViewModel;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GenreManagementFragment extends Fragment {
    private RecyclerView rvAllGenres;
    private MaterialButton btnAddGenre;
    private AdminGenreAdapter genreAdapter;
    private List<Genre> genreList;
    private GenreViewModel viewModel;

    // Biến cho Dialog
    private AlertDialog currentDialog;
    private Uri selectedImageUri = null;
    private ImageView currentDialogImageView = null;
    private MaterialButton btnSaveCurrent; // Lưu tạm nút Save để reset trạng thái

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (currentDialogImageView != null) {
                        Glide.with(requireContext()).load(selectedImageUri).into(currentDialogImageView);
                    }
                }
            }
    );

    public GenreManagementFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_genre, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAllGenres = view.findViewById(R.id.rvGenres);
        btnAddGenre = view.findViewById(R.id.btnAddGenre);

        setupRecyclerView();

        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(GenreViewModel.class);

        // 1. Observe Danh sách thể loại
        observeGenreList();

        // 2. Observe Kết quả Trạng thái (Thêm/Sửa/Xóa thành công hay không)
        viewModel.getActionSuccess().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess != null) {
                if (isSuccess) {
                    // Thành công -> Đóng Dialog (nếu đang mở) và Load lại danh sách
                    if (currentDialog != null && currentDialog.isShowing()) {
                        currentDialog.dismiss();
                    }
                    viewModel.refreshGenres();
                    observeGenreList(); // Gắn lại Observer cho list mới
                } else {
                    // Thất bại -> Bật lại nút Save cho bấm lại
                    if (btnSaveCurrent != null) {
                        btnSaveCurrent.setEnabled(true);
                        btnSaveCurrent.setText("Lưu lại");
                    }
                }
            }
        });

        // 3. Observe Thông báo lỗi/thành công để hiện Toast
        viewModel.getActionMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        btnAddGenre.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void observeGenreList() {
        viewModel.getAllGenres().observe(getViewLifecycleOwner(), genres -> {
            if (genres != null) {
                genreList.clear();
                genreList.addAll(genres);
                genreAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setupRecyclerView() {
        rvAllGenres.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        genreList = new ArrayList<>();
        // Truyền context, list và bắt sự kiện từ Adapter (bạn tự custom theo Adapter của bạn)
        genreAdapter = new AdminGenreAdapter(getContext(), genreList, genre -> {
            // Khi bấm vào 1 Thể loại -> Mở hộp thoại Edit lên
            showAddEditDialog(genre);
        });
        rvAllGenres.setAdapter(genreAdapter);
    }

    public void showAddEditDialog(Genre genre) {
        selectedImageUri = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_edit_genre, null);
        builder.setView(view);
        currentDialog = builder.create();

        if (currentDialog.getWindow() != null) {
            currentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextInputEditText edtName = view.findViewById(R.id.edtGenreName);
        ImageView imgPreview = view.findViewById(R.id.imgGenrePreview);
        MaterialButton btnPickImage = view.findViewById(R.id.btnPickImage);
        btnSaveCurrent = view.findViewById(R.id.btnSaveGenre);

        // TÌM NÚT XÓA BẠN VỪA THÊM TRONG XML
        MaterialButton btnDelete = view.findViewById(R.id.btnDeleteGenre);

        currentDialogImageView = imgPreview;
        boolean isEdit = (genre != null);

        if (isEdit) {
            tvTitle.setText("Edit Genre");
            edtName.setText(genre.getName());
            if (genre.getCoverUrl() != null && !genre.getCoverUrl().isEmpty()) {
                Glide.with(requireContext()).load(genre.getCoverUrl()).into(imgPreview);
            }

            if (btnDelete != null) {
                btnDelete.setVisibility(View.VISIBLE);

                // KIỂM TRA TRẠNG THÁI ẨN/HIỆN ĐỂ TÙY BIẾN NÚT
                if (!genre.isVisible()) {
                    // Trạng thái: ĐÃ BỊ ẨN -> Đổi thành nút "Restore" (Màu xanh)
                    btnDelete.setText("Restore");
                    btnDelete.setTextColor(android.graphics.Color.parseColor("#1DB954")); // Xanh Spotify
                    btnDelete.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1DB954")));

                    btnDelete.setOnClickListener(v -> {
                        currentDialog.dismiss();
                        restoreGenre(genre); // Gọi hàm khôi phục
                    });
                } else {
                    // Trạng thái: BÌNH THƯỜNG -> Nút "Delete" (Màu đỏ)
                    btnDelete.setText("Delete");
                    btnDelete.setTextColor(android.graphics.Color.parseColor("#FF3B30"));
                    btnDelete.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF3B30")));

                    btnDelete.setOnClickListener(v -> {
                        currentDialog.dismiss();
                        softDeleteGenre(genre); // Gọi hàm xóa mềm
                    });
                }
            }
        } else {
            tvTitle.setText("Add New Genre");
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
        }

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // Xử lý nút SAVE (giữ nguyên logic của bạn)
        btnSaveCurrent.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            if (name.isEmpty()) {
                edtName.setError("Không được để trống");
                return;
            }

            btnSaveCurrent.setEnabled(false);
            btnSaveCurrent.setText("Đang xử lý...");

            byte[] imageBytes = null;
            if (selectedImageUri != null) {
                imageBytes = getBytesFromUri(selectedImageUri);
            }

            viewModel.saveGenre(
                    isEdit ? genre.getId() : null,
                    name,
                    isEdit ? genre.getCoverUrl() : "",
                    imageBytes
            );
        });

        currentDialog.show();
    }

    public void softDeleteGenre(Genre genre) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Ẩn thể loại " + genre.getName() + " ?")
                .setPositiveButton("Xóa", (dialog, which) -> viewModel.deleteGenre(genre.getId()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    public void restoreGenre(Genre genre) {
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận khôi phục")
                .setMessage("Khôi phục hiển thị thể loại " + genre.getName() + " ?")
                .setPositiveButton("Khôi phục", (dialog, which) -> {
                    // Gọi hàm cập nhật trong ViewModel (Ví dụ: truyền cờ true để mở lại)
                    // Tùy theo cách bạn viết API, có thể là viewModel.restoreGenre(...) hoặc viewModel.updateVisibility(...)
                    viewModel.restoreGenre(genre.getId());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private byte[] getBytesFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}