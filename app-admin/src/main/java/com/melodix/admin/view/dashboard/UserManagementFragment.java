package com.melodix.admin.view.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melodix.admin.R;
import com.melodix.admin.viewmodel.AdminUserViewModel;
import com.melodix.core.model.Profile;
import com.melodix.core.utils.LoadingDialog;
import java.util.ArrayList;
import java.util.List;

public class UserManagementFragment extends Fragment {

    private RecyclerView rvAllUsers;
    private AutoCompleteTextView actvRole;
    private AdminUserViewModel viewModel;
    private LoadingDialog loadingDialog;
    private List<Profile> fullUserList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadingDialog = new LoadingDialog();
        rvAllUsers = view.findViewById(R.id.rvAllUsers);
        actvRole = view.findViewById(R.id.actvRole);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        viewModel = new ViewModelProvider(this).get(AdminUserViewModel.class);
        loadingDialog.showLoading(requireActivity());
        
        viewModel.getAllProfiles().observe(getViewLifecycleOwner(), profiles -> {
            loadingDialog.hideLoading();
            if (profiles != null) {
                this.fullUserList = profiles;
                // Update adapter logic here
            }
        });
    }
}