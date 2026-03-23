package com.melodix.app.Model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Album implements Serializable {

    private String id;
    private String title;
    private String artistName;
    private String coverUrl;
    private Timestamp releaseTimestamp;
    private boolean active;

    public Album() {
    }

    public static Album fromDocument(DocumentSnapshot document) {
        Album album = new Album();

        if (document == null || !document.exists()) {
            return album;
        }

        try {
            album.setId(document.getId());
            album.setTitle(getSafeString(document, "title"));
            album.setArtistName(getSafeString(document, "artistName"));
            album.setCoverUrl(getSafeString(document, "coverUrl"));
            album.setReleaseTimestamp(document.getTimestamp("releaseTimestamp"));

            Boolean activeValue = document.getBoolean("active");
            album.setActive(activeValue == null || activeValue);
        } catch (Exception ignored) {
        }

        return album;
    }

    private static String getSafeString(DocumentSnapshot document, String key) {
        try {
            String value = document.getString(key);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        return "Untitled Album";
    }

    public String getReleaseYear() {
        try {
            if (releaseTimestamp == null || releaseTimestamp.toDate() == null) {
                return "";
            }
            return new SimpleDateFormat("yyyy", Locale.getDefault()).format(releaseTimestamp.toDate());
        } catch (Exception e) {
            return "";
        }
    }

    public String getDisplaySubtitle() {
        String artist = (artistName == null || artistName.trim().isEmpty()) ? "Unknown Artist" : artistName;
        String year = getReleaseYear();

        if (!year.isEmpty()) {
            return artist + " • " + year;
        }

        return artist;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName == null ? "" : artistName;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl == null ? "" : coverUrl;
    }

    public Timestamp getReleaseTimestamp() {
        return releaseTimestamp;
    }

    public void setReleaseTimestamp(Timestamp releaseTimestamp) {
        this.releaseTimestamp = releaseTimestamp;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}