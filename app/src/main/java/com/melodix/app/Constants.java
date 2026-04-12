package com.melodix.app;

public class Constants {
    // Đường dẫn gốc của Storage (Nhớ có dấu gạch chéo ở cuối)
    public static final String STORAGE_BASE_URL = "https://ggektdtrjagrmfnimmaw.supabase.co/storage/v1/object/public/";
    public static final String EDGE_FUNCTION_URL = "functions/v1/";

    // Tên các Bucket
    public static final String AVATAR_BUCKET = "avatar_user/";
    public static final String SONG_COVER_BUCKET = "cover_song/";
    public static final String SONG_AUDIO_BUCKET = "song/";

    public static final String GENRE_COVER_BUCKET = "genre_cover/";


    // Tên các edge function

    public static final String SUMMARIZE_COMMENT = "summarize-comments";
}