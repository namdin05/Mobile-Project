package com.melodix.app.Model;

import com.google.gson.annotations.SerializedName;

public class SignUpRequest {
    private String email;
    private String password;

    private UserData data;

    public SignUpRequest(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.data = new UserData(fullName);
    }

    private static class UserData {
        @SerializedName("display_name")
        private String displayName;
        public UserData(String displayName) {
            this.displayName = displayName;
        }
    }
}
