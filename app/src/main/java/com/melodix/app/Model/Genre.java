package com.melodix.app.Model;

import com.google.firebase.firestore.DocumentSnapshot;

import java.io.Serializable;

public class Genre implements Serializable {

    private String id;
    private String name;
    private String imageUrl;
    private boolean active;

    public Genre() {
    }

    public static Genre fromDocument(DocumentSnapshot document) {
        Genre genre = new Genre();

        if (document == null || !document.exists()) {
            return genre;
        }

        try {
            genre.setId(document.getId());
            genre.setName(getSafeString(document, "name"));
            genre.setImageUrl(getSafeString(document, "imageUrl"));

            Boolean activeValue = document.getBoolean("active");
            genre.setActive(activeValue == null || activeValue);
        } catch (Exception ignored) {
        }

        return genre;
    }

    private static String getSafeString(DocumentSnapshot document, String key) {
        try {
            String value = document.getString(key);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return "Thể loại";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl == null ? "" : imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}