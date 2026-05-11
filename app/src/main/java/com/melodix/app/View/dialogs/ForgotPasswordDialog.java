package com.melodix.app.View.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.melodix.app.R;
import com.melodix.app.Utils.LoadingDialog;
import com.melodix.app.ViewModel.AuthViewModel;

public class ForgotPasswordDialog extends DialogFragment {

    private AuthViewModel authViewModel;
    private LoadingDialog loadingDialog;

    public static ForgotPasswordDialog newInstance() {
        return new ForgotPasswordDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        loadingDialog = new LoadingDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText edtEmail = view.findViewById(R.id.edt_forgot_email);

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.btn_send).setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (email.isEmpty()) {
                edtEmail.setError("Vui lòng nhập email");
                return;
            }

            loadingDialog.showLoading(requireActivity());
            authViewModel.resetPassword(email).observe(getViewLifecycleOwner(), result -> {
                loadingDialog.hideLoading();
                if ("SUCCESS".equals(result)) {
                    Toast.makeText(requireContext(), "Vui lòng kiểm tra email để đặt lại mật khẩu!", Toast.LENGTH_LONG).show();
                    dismiss();
                } else {
                    Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setLayout(width, height);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}
