package com.melodix.app.Repository;

import android.content.Context;
import android.net.Uri;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.melodix.app.Model.UserProfile;
import com.melodix.app.Utils.AppConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class AuthRepository {

    private static final String TAG = "AuthRepository";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firebaseFirestore;
    private final CollectionReference usersCollection;

    public AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        usersCollection = firebaseFirestore.collection(AppConstants.USERS_COLLECTION);
    }

    public FirebaseUser getCurrentFirebaseUser() {
        try {
            return firebaseAuth.getCurrentUser();
        } catch (Exception e) {
            Log.e(TAG, "getCurrentFirebaseUser failed.", e);
            return null;
        }
    }

    public void signInWithEmail(String email, String password, @NonNull RepositoryCallback<UserProfile> callback) {
        try {
            firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            callback.onError(mapException(task.getException()));
                            return;
                        }

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            callback.onError("Đăng nhập thành công nhưng không lấy được thông tin người dùng.");
                            return;
                        }

                        createOrMergeUserProfile(
                                user,
                                user.getDisplayName(),
                                AppConstants.PROVIDER_EMAIL,
                                callback
                        );
                    });
        } catch (Exception e) {
            Log.e(TAG, "signInWithEmail exception.", e);
            callback.onError("Không thể đăng nhập bằng email lúc này.");
        }
    }

    public void registerWithEmail(String fullName, String email, String password, @NonNull RepositoryCallback<UserProfile> callback) {
        try {
            firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            callback.onError(mapException(task.getException()));
                            return;
                        }

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            callback.onError("Tạo tài khoản thành công nhưng không lấy được thông tin người dùng.");
                            return;
                        }

                        try {
                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName.trim())
                                    .build();

                            user.updateProfile(request)
                                    .addOnCompleteListener(updateTask -> createOrMergeUserProfile(
                                            user,
                                            fullName,
                                            AppConstants.PROVIDER_EMAIL,
                                            callback
                                    ));
                        } catch (Exception e) {
                            Log.e(TAG, "updateProfile failed, continue anyway.", e);
                            createOrMergeUserProfile(
                                    user,
                                    fullName,
                                    AppConstants.PROVIDER_EMAIL,
                                    callback
                            );
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "registerWithEmail exception.", e);
            callback.onError("Không thể tạo tài khoản lúc này.");
        }
    }

    public void signInWithGoogle(String idToken, @NonNull RepositoryCallback<UserProfile> callback) {
        try {
            if (TextUtils.isEmpty(idToken)) {
                callback.onError("Google ID token không hợp lệ.");
                return;
            }

            AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            callback.onError(mapException(task.getException()));
                            return;
                        }

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            callback.onError("Đăng nhập Google thành công nhưng không lấy được thông tin người dùng.");
                            return;
                        }

                        createOrMergeUserProfile(
                                user,
                                user.getDisplayName(),
                                AppConstants.PROVIDER_GOOGLE,
                                callback
                        );
                    });
        } catch (Exception e) {
            Log.e(TAG, "signInWithGoogle exception.", e);
            callback.onError("Không thể đăng nhập bằng Google lúc này.");
        }
    }

    public void signInWithFacebook(String accessToken, @NonNull RepositoryCallback<UserProfile> callback) {
        try {
            if (TextUtils.isEmpty(accessToken)) {
                callback.onError("Facebook access token không hợp lệ.");
                return;
            }

            AuthCredential credential = FacebookAuthProvider.getCredential(accessToken);
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            callback.onError(mapException(task.getException()));
                            return;
                        }

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            callback.onError("Đăng nhập Facebook thành công nhưng không lấy được thông tin người dùng.");
                            return;
                        }

                        createOrMergeUserProfile(
                                user,
                                user.getDisplayName(),
                                AppConstants.PROVIDER_FACEBOOK,
                                callback
                        );
                    });
        } catch (Exception e) {
            Log.e(TAG, "signInWithFacebook exception.", e);
            callback.onError("Không thể đăng nhập bằng Facebook lúc này.");
        }
    }

    public void getCurrentUserProfile(@NonNull RepositoryCallback<UserProfile> callback) {
        try {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser == null) {
                callback.onError("Phiên đăng nhập đã hết. Vui lòng đăng nhập lại.");
                return;
            }

            usersCollection.document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        try {
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                callback.onSuccess(UserProfile.fromDocument(documentSnapshot));
                            } else {
                                createOrMergeUserProfile(
                                        currentUser,
                                        currentUser.getDisplayName(),
                                        resolvePrimaryProvider(currentUser),
                                        callback
                                );
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "getCurrentUserProfile parse failed.", e);
                            callback.onSuccess(buildLocalUserProfile(
                                    currentUser,
                                    currentUser.getDisplayName(),
                                    resolvePrimaryProvider(currentUser),
                                    null
                            ));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "getCurrentUserProfile firestore failed.", e);
                        callback.onSuccess(buildLocalUserProfile(
                                currentUser,
                                currentUser.getDisplayName(),
                                resolvePrimaryProvider(currentUser),
                                null
                        ));
                    });
        } catch (Exception e) {
            Log.e(TAG, "getCurrentUserProfile exception.", e);
            callback.onError("Không thể tải hồ sơ người dùng.");
        }
    }

    public void signOut(@NonNull Context context, @NonNull RepositoryCallback<Boolean> callback) {
        try {
            firebaseAuth.signOut();
        } catch (Exception e) {
            Log.e(TAG, "Firebase signOut error.", e);
        }

        try {
            LoginManager.getInstance().logOut();
        } catch (Exception e) {
            Log.e(TAG, "Facebook LoginManager logout error.", e);
        }

        try {
            AccessToken.setCurrentAccessToken(null);
        } catch (Exception e) {
            Log.e(TAG, "AccessToken clear error.", e);
        }

        try {
            Profile.setCurrentProfile(null);
        } catch (Exception e) {
            Log.e(TAG, "Facebook Profile clear error.", e);
        }

        try {
            CredentialManager credentialManager = CredentialManager.create(context.getApplicationContext());
            ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();

            credentialManager.clearCredentialStateAsync(
                    clearRequest,
                    new CancellationSignal(),
                    Executors.newSingleThreadExecutor(),
                    new CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override
                        public void onResult(@NonNull Void result) {
                            callback.onSuccess(Boolean.TRUE);
                        }

                        @Override
                        public void onError(@NonNull ClearCredentialException e) {
                            Log.e(TAG, "clearCredentialStateAsync failed.", e);
                            callback.onSuccess(Boolean.TRUE);
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "CredentialManager signOut cleanup exception.", e);
            callback.onSuccess(Boolean.TRUE);
        }
    }

    private void createOrMergeUserProfile(@NonNull FirebaseUser firebaseUser,
                                          String displayNameOverride,
                                          String provider,
                                          @NonNull RepositoryCallback<UserProfile> callback) {
        try {
            usersCollection.document(firebaseUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        try {
                            Map<String, Object> data = buildUserMap(
                                    firebaseUser,
                                    documentSnapshot,
                                    displayNameOverride,
                                    provider
                            );

                            usersCollection.document(firebaseUser.getUid())
                                    .set(data, SetOptions.merge())
                                    .addOnSuccessListener(unused -> loadUserProfileByUid(
                                            firebaseUser.getUid(),
                                            firebaseUser,
                                            displayNameOverride,
                                            provider,
                                            callback
                                    ))
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "save user profile failed.", e);
                                        callback.onSuccess(buildLocalUserProfile(
                                                firebaseUser,
                                                displayNameOverride,
                                                provider,
                                                documentSnapshot
                                        ));
                                    });
                        } catch (Exception e) {
                            Log.e(TAG, "createOrMergeUserProfile inner exception.", e);
                            callback.onSuccess(buildLocalUserProfile(
                                    firebaseUser,
                                    displayNameOverride,
                                    provider,
                                    documentSnapshot
                            ));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "read existing user profile failed.", e);
                        callback.onSuccess(buildLocalUserProfile(
                                firebaseUser,
                                displayNameOverride,
                                provider,
                                null
                        ));
                    });
        } catch (Exception e) {
            Log.e(TAG, "createOrMergeUserProfile exception.", e);
            callback.onSuccess(buildLocalUserProfile(
                    firebaseUser,
                    displayNameOverride,
                    provider,
                    null
            ));
        }
    }

    private void loadUserProfileByUid(@NonNull String uid,
                                      @NonNull FirebaseUser firebaseUser,
                                      String displayNameOverride,
                                      String provider,
                                      @NonNull RepositoryCallback<UserProfile> callback) {
        try {
            usersCollection.document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            callback.onSuccess(UserProfile.fromDocument(documentSnapshot));
                        } else {
                            callback.onSuccess(buildLocalUserProfile(
                                    firebaseUser,
                                    displayNameOverride,
                                    provider,
                                    null
                            ));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "loadUserProfileByUid failed.", e);
                        callback.onSuccess(buildLocalUserProfile(
                                firebaseUser,
                                displayNameOverride,
                                provider,
                                null
                        ));
                    });
        } catch (Exception e) {
            Log.e(TAG, "loadUserProfileByUid exception.", e);
            callback.onSuccess(buildLocalUserProfile(
                    firebaseUser,
                    displayNameOverride,
                    provider,
                    null
            ));
        }
    }

    private Map<String, Object> buildUserMap(@NonNull FirebaseUser firebaseUser,
                                             DocumentSnapshot existingSnapshot,
                                             String displayNameOverride,
                                             String provider) {
        Map<String, Object> map = new HashMap<>();

        String resolvedName = resolveDisplayName(firebaseUser, displayNameOverride, existingSnapshot);
        String resolvedRole = resolveRole(existingSnapshot);
        String resolvedProvider = !TextUtils.isEmpty(provider) ? provider : resolvePrimaryProvider(firebaseUser);

        String existingPhotoUrl = "";
        if (existingSnapshot != null && existingSnapshot.exists()) {
            try {
                String photo = existingSnapshot.getString("photoUrl");
                existingPhotoUrl = photo == null ? "" : photo;
            } catch (Exception ignored) {
            }
        }

        Uri photoUri = firebaseUser.getPhotoUrl();
        String photoUrl = photoUri != null ? photoUri.toString() : existingPhotoUrl;

        map.put("uid", firebaseUser.getUid());
        map.put("fullName", resolvedName);
        map.put("email", safeString(firebaseUser.getEmail()));
        map.put("photoUrl", photoUrl);
        map.put("role", resolvedRole);
        map.put("provider", resolvedProvider);
        map.put("active", true);
        map.put("emailVerified", firebaseUser.isEmailVerified());
        map.put("updatedAt", FieldValue.serverTimestamp());
        map.put("lastLoginAt", FieldValue.serverTimestamp());

        if (existingSnapshot == null || !existingSnapshot.exists()) {
            map.put("createdAt", FieldValue.serverTimestamp());
        }

        return map;
    }

    private UserProfile buildLocalUserProfile(@NonNull FirebaseUser firebaseUser,
                                              String displayNameOverride,
                                              String provider,
                                              DocumentSnapshot existingSnapshot) {
        UserProfile profile = new UserProfile();

        try {
            profile.setUid(firebaseUser.getUid());
            profile.setFullName(resolveDisplayName(firebaseUser, displayNameOverride, existingSnapshot));
            profile.setEmail(safeString(firebaseUser.getEmail()));

            Uri photoUri = firebaseUser.getPhotoUrl();
            if (photoUri != null) {
                profile.setPhotoUrl(photoUri.toString());
            } else if (existingSnapshot != null && existingSnapshot.exists()) {
                String localPhoto = existingSnapshot.getString("photoUrl");
                profile.setPhotoUrl(localPhoto == null ? "" : localPhoto);
            } else {
                profile.setPhotoUrl("");
            }

            profile.setRole(resolveRole(existingSnapshot));
            profile.setProvider(TextUtils.isEmpty(provider) ? resolvePrimaryProvider(firebaseUser) : provider);
            profile.setActive(true);
            profile.setEmailVerified(firebaseUser.isEmailVerified());

            if (existingSnapshot != null && existingSnapshot.exists()) {
                profile.setCreatedAt(existingSnapshot.getTimestamp("createdAt"));
                profile.setUpdatedAt(existingSnapshot.getTimestamp("updatedAt"));
                profile.setLastLoginAt(existingSnapshot.getTimestamp("lastLoginAt"));
            }
        } catch (Exception e) {
            Log.e(TAG, "buildLocalUserProfile exception.", e);
        }

        return profile;
    }

    private String resolveDisplayName(@NonNull FirebaseUser firebaseUser,
                                      String displayNameOverride,
                                      DocumentSnapshot existingSnapshot) {
        try {
            if (!TextUtils.isEmpty(displayNameOverride)) {
                return displayNameOverride.trim();
            }

            if (!TextUtils.isEmpty(firebaseUser.getDisplayName())) {
                return firebaseUser.getDisplayName().trim();
            }

            if (existingSnapshot != null && existingSnapshot.exists()) {
                String existingName = existingSnapshot.getString("fullName");
                if (!TextUtils.isEmpty(existingName)) {
                    return existingName.trim();
                }
            }

            String email = firebaseUser.getEmail();
            if (!TextUtils.isEmpty(email) && email.contains("@")) {
                return email.substring(0, email.indexOf("@"));
            }
        } catch (Exception e) {
            Log.e(TAG, "resolveDisplayName error.", e);
        }

        return "Melodix User";
    }

    private String resolveRole(DocumentSnapshot existingSnapshot) {
        try {
            if (existingSnapshot != null && existingSnapshot.exists()) {
                String role = existingSnapshot.getString("role");
                if (!TextUtils.isEmpty(role)) {
                    return role;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "resolveRole error.", e);
        }

        return AppConstants.ROLE_USER;
    }

    private String resolvePrimaryProvider(@NonNull FirebaseUser firebaseUser) {
        try {
            List<? extends UserInfo> providerData = firebaseUser.getProviderData();
            if (providerData != null) {
                for (UserInfo info : providerData) {
                    if (info != null && info.getProviderId() != null && !"firebase".equals(info.getProviderId())) {
                        return info.getProviderId();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "resolvePrimaryProvider error.", e);
        }

        return AppConstants.PROVIDER_EMAIL;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String mapException(Exception exception) {
        if (exception == null) {
            return "Đã xảy ra lỗi không xác định.";
        }

        if (exception instanceof FirebaseTooManyRequestsException) {
            return "Bạn thao tác quá nhanh. Vui lòng thử lại sau.";
        }

        if (exception instanceof FirebaseNetworkException) {
            return "Lỗi mạng. Hãy kiểm tra kết nối Internet.";
        }

        if (exception instanceof FirebaseAuthUserCollisionException) {
            return "Email này đã được đăng ký.";
        }

        if (exception instanceof FirebaseAuthInvalidUserException) {
            return "Tài khoản không tồn tại hoặc đã bị vô hiệu hóa.";
        }

        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            String message = exception.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("badly formatted")) {
                    return "Email không đúng định dạng.";
                }
                if (lower.contains("password is invalid") || lower.contains("the password is invalid")) {
                    return "Mật khẩu không đúng.";
                }
                if (lower.contains("no user record")) {
                    return "Tài khoản không tồn tại.";
                }
                if (lower.contains("supplied auth credential is malformed")) {
                    return "Thông tin đăng nhập không hợp lệ.";
                }
            }
            return "Thông tin đăng nhập không hợp lệ.";
        }

        String message = exception.getMessage();
        if (!TextUtils.isEmpty(message)) {
            String lower = message.toLowerCase();

            if (lower.contains("already exists with the same email address")) {
                return "Email này đã tồn tại bằng phương thức đăng nhập khác.";
            }

            if (lower.contains("password should be at least 6 characters")) {
                return "Mật khẩu phải có ít nhất 6 ký tự.";
            }

            if (lower.contains("network")) {
                return "Lỗi mạng. Hãy kiểm tra kết nối Internet.";
            }

            return message;
        }

        return "Đã xảy ra lỗi không xác định.";
    }
}