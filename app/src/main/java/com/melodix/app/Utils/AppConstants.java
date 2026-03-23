package com.melodix.app.Utils;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String USERS_COLLECTION = "users";
    public static final String HOME_BANNERS_COLLECTION = "home_banners";
    public static final String SONGS_COLLECTION = "songs";
    public static final String ALBUMS_COLLECTION = "albums";
    public static final String GENRES_COLLECTION = "genres";
    public static final String LYRICS_COLLECTION = "lyrics";

    public static final String ROLE_USER = "user";
    public static final String ROLE_ARTIST = "artist";
    public static final String ROLE_ADMIN = "admin";

    public static final String PROVIDER_EMAIL = "password";
    public static final String PROVIDER_GOOGLE = "google.com";
    public static final String PROVIDER_FACEBOOK = "facebook.com";

    public static final int HOME_BANNER_LIMIT = 6;
    public static final int HOME_TRENDING_LIMIT = 12;
    public static final int HOME_GENRE_LIMIT = 12;
    public static final int HOME_ALBUM_LIMIT = 12;
    public static final int HOME_QUERY_BUFFER_LIMIT = 30;

    public static final int PLAYER_NOTIFICATION_ID = 11001;
    public static final String PLAYER_NOTIFICATION_CHANNEL_ID = "melodix_media_playback_channel";

    public static final long PLAYER_SEEK_INTERVAL_MS = 15000L;
    public static final float PLAYER_MIN_SPEED = 0.5f;
    public static final float PLAYER_MAX_SPEED = 2.0f;
    public static final float PLAYER_DEFAULT_SPEED = 1.0f;
}