package com.melodix.app.ViewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.melodix.app.Data.Resource;
import com.melodix.app.Model.UserProfile;
import com.melodix.app.Repository.AuthRepository;
import com.melodix.app.Repository.RepositoryCallback;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<Resource<UserProfile>> authState =
            new MutableLiveData<>(Resource.<UserProfile>idle());

    private final MutableLiveData<Resource<UserProfile>> profileState =
            new MutableLiveData<>(Resource.<UserProfile>idle());

    private final MutableLiveData<Resource<Boolean>> signOutState =
            new MutableLiveData<>(Resource.<Boolean>idle());

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository();
    }

    public LiveData<Resource<UserProfile>> getAuthState() {
        return authState;
    }

    public LiveData<Resource<UserProfile>> getProfileState() {
        return profileState;
    }

    public LiveData<Resource<Boolean>> getSignOutState() {
        return signOutState;
    }

    public void loginWithEmail(String email, String password) {
        authState.postValue(Resource.<UserProfile>loading());

        authRepository.signInWithEmail(email, password, new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile data) {
                authState.postValue(Resource.success(data));
            }

            @Override
            public void onError(String message) {
                authState.postValue(Resource.error(message));
            }
        });
    }

    public void registerWithEmail(String fullName, String email, String password) {
        authState.postValue(Resource.<UserProfile>loading());

        authRepository.registerWithEmail(fullName, email, password, new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile data) {
                authState.postValue(Resource.success(data));
            }

            @Override
            public void onError(String message) {
                authState.postValue(Resource.error(message));
            }
        });
    }

    public void loginWithGoogle(String idToken) {
        authState.postValue(Resource.<UserProfile>loading());

        authRepository.signInWithGoogle(idToken, new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile data) {
                authState.postValue(Resource.success(data));
            }

            @Override
            public void onError(String message) {
                authState.postValue(Resource.error(message));
            }
        });
    }

    public void loginWithFacebook(String accessToken) {
        authState.postValue(Resource.<UserProfile>loading());

        authRepository.signInWithFacebook(accessToken, new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile data) {
                authState.postValue(Resource.success(data));
            }

            @Override
            public void onError(String message) {
                authState.postValue(Resource.error(message));
            }
        });
    }

    public void loadCurrentUserProfile() {
        profileState.postValue(Resource.<UserProfile>loading());

        authRepository.getCurrentUserProfile(new RepositoryCallback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile data) {
                profileState.postValue(Resource.success(data));
            }

            @Override
            public void onError(String message) {
                profileState.postValue(Resource.error(message));
            }
        });
    }

    public void signOut() {
        signOutState.postValue(Resource.<Boolean>loading());

        authRepository.signOut(getApplication().getApplicationContext(), new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean data) {
                signOutState.postValue(Resource.success(Boolean.TRUE));
            }

            @Override
            public void onError(String message) {
                signOutState.postValue(Resource.error(message));
            }
        });
    }

    public void resetAuthState() {
        authState.postValue(Resource.<UserProfile>idle());
    }

    public void resetProfileState() {
        profileState.postValue(Resource.<UserProfile>idle());
    }

    public void resetSignOutState() {
        signOutState.postValue(Resource.<Boolean>idle());
    }
}