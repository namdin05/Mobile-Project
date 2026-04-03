package com.melodix.app.View.admin;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.melodix.app.Adapter.AdminGenreAdapter;
import com.melodix.app.Model.Genre;
import com.melodix.app.R;
import com.melodix.app.ViewModel.admin.AdminGenreViewModel;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AdminGenreFragment extends Fragment {

    private RecyclerView rvGenres;
    private MaterialButton btnAddGenre;
    private AdminGenreAdapter adapter;
    private List<Genre> genreList;
    private AdminGenreViewModel viewModel;

    // Biến cho Dialog
    private AlertDialog currentDialog;
    private Uri selectedImageUri = null;
    private ImageView currentDialogImageView = null;

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

    public AdminGenreFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_genre, container, false);

        rvGenres = view.findViewById(R.id.rvGenres);
        btnAddGenre = view.findViewById(R.id.btnAddGenre);

        // Setup UI
        rvGenres.setLayoutManager(new LinearLayoutManager(getContext()));
        genreList = new ArrayList<>();
        adapter = new AdminGenreAdapter(getContext(), genreList);
        rvGenres.setAdapter(adapter);

        // Setup ViewModel
        viewModel = new ViewModelProvider(this).get(AdminGenreViewModel.class);

        // Lắng nghe dữ liệu
        viewModel.getGenresLiveData().observe(getViewLifecycleOwner(), genres -> {
            genreList.clear();
            genreList.addAll(genres);
            adapter.notifyDataSetChanged();
        });

        // Lắng nghe thông báo
        viewModel.getMessageLiveData().observe(getViewLifecycleOwner(), message -> {
            if (message != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });

        // Lắng nghe trạng thái thành công để đóng Dialog
        viewModel.getIsSuccessLiveData().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess && currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dismiss();
            }
        });

        // Kéo data lần đầu
        viewModel.loadGenres();

        // Nút thêm mới
        btnAddGenre.setOnClickListener(v -> showAddEditDialog(null));

        return view;
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
        MaterialButton btnSave = view.findViewById(R.id.btnSaveGenre);

        currentDialogImageView = imgPreview;
        boolean isEdit = (genre != null);

        if (isEdit) {
            tvTitle.setText("Edit Genre");
            edtName.setText(genre.getName());
            if (genre.getCoverUrl() != null && !genre.getCoverUrl().isEmpty()) {
                Glide.with(requireContext()).load(genre.getCoverUrl()).into(imgPreview);
            }
        }

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            if (name.isEmpty()) {
                edtName.setError("Không được để trống");
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("Đang xử lý...");

            byte[] imageBytes = null;
            if (selectedImageUri != null) {
                imageBytes = getBytesFromUri(selectedImageUri);
            }

            // Đẩy toàn bộ cục dữ liệu qua ViewModel lo liệu
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

    // Hàm phụ trợ: Chuyển Uri ảnh thành mảng Byte
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