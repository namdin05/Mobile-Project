package com.melodix.app.View.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;

import com.melodix.app.Model.Profile;
import com.melodix.app.Model.SessionManager;
import com.melodix.app.R;
import com.melodix.app.Repository.auth.AuthRepository;

public class HomeFragment extends Fragment {
    Profile cur_user;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        AuthRepository repository = new AuthRepository();

        // in du lieu ra logcat, getViewLifecycleOwner giup ham chi chay khi user dang mo fragment
        // bien songs co kieu du lieu dua theo du lieu tra ve cua ham fetchNewReleaseSongs
        repository.fetchNewReleaseSongs().observe(getViewLifecycleOwner(), songs -> {
            if(songs != null && !songs.isEmpty()){
                for (int i = 0; i < songs.size(); i++){
                    Log.d("test_song", songs.get(i).getTitle());
                }
            } else {
                Log.d("test_song", "thai bai");
            }

        });

        cur_user = SessionManager.getInstance(requireContext()).getCurrentUser();
        Log.d("get_session_user", cur_user.getDisplayName()+" hello");
        return view;
    }
}
