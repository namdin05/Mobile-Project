package com.melodix.app.Model;

import java.io.Serializable;
import java.util.ArrayList;

public class AppUser implements Serializable {
    public String id;
    public String displayName;
    public String username;
    public String email;
    public String passwordHash;
    public String role;
    public String avatarRes;
    public String headline;
    public String bio;
    public boolean darkMode;
    public boolean offlineMode;
    public boolean suspended;
    public ArrayList<String> playlistIds = new ArrayList<>();
    public ArrayList<String> likedSongIds = new ArrayList<>();
    public ArrayList<String> followingArtistIds = new ArrayList<>();
    public ArrayList<String> downloadedSongIds = new ArrayList<>();
    public ArrayList<String> recentSearches = new ArrayList<>();
    public ArrayList<String> recentSongIds = new ArrayList<>();
    public ArrayList<String> badges = new ArrayList<>();

    public AppUser() {
    }

    public AppUser(String id, String displayName, String username, String email, String passwordHash,
                   String role, String avatarRes, String headline, String bio) {
        this.id = id;
        this.displayName = displayName;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.avatarRes = avatarRes;
        this.headline = headline;
        this.bio = bio;
    }
}
