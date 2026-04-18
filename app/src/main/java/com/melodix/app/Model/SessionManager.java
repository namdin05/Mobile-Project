package com.melodix.app.Model;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;

public class SessionManager {
    private static SessionManager instance;
    private Profile currentUser;
    private String token;
    private SharedPreferences prefs;
    private Gson gson;

    private SessionManager(Context context){
        // su dung getApplicationContext de tranh memory leak vi neu gan context voi 1 screen cu the se lam screen do k bi xoa khoi RAM
        prefs = context.getApplicationContext().getSharedPreferences("SESSION", 0);
        gson = new Gson();
        String userJson = prefs.getString("USER_PROFILE", null);
        if (userJson != null) this.currentUser = gson.fromJson(userJson, Profile.class);
        this.token = prefs.getString("TOKEN", null); // getString o constructor de moi lan mo app, de nap du lieu neu bo nho co du lieu
    }

    public static SessionManager getInstance(Context context){
        if(instance == null){
            instance = new SessionManager(context);
        }
        return instance;
    }

    public Profile getCurrentUser(){
        return currentUser;
    }

    public String getToken() {
        return token;
    }

    public void saveLogInSession(Profile user, String token){
        this.currentUser = user;
        this.token = token;
        //chuyen Profile object thanh json
        String userJson = gson.toJson(user);
        prefs.edit().putString("TOKEN", token).putString("USER_PROFILE", userJson).apply();
    }
}
