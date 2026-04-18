package com.melodix.app.Model;

import android.content.Context;

import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Song {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("album_id")
    private String album_id;

    @SerializedName("cover_url")
    private String cover_url;

    @SerializedName("audio_url")
    private String audio_url;

    @SerializedName("lyrics_lrc_url")
    private String lyrics_url;

    @SerializedName("duration_seconds")
    private int duration_seconds;

    @SerializedName("stream_count")
    private int plays;

    @SerializedName("status")
    private String status;

    // ĐÃ THÊM SERIALIZED NAME Ở ĐÂY ĐỂ NHẬN CHUỖI GỘP NHIỀU NGHỆ SĨ
    @SerializedName("artistName")
    public String artistName;

    // THÊM SERIALIZED NAME ĐỂ NHẬN SỐ LƯỢT THÍCH TỪ SUPABASE
    @SerializedName("like_count")
    private int likes;

    private String artistId;
    private String albumName;
    private String genre;
    private String description;

    private ArrayList<LyricLine> lyrics = new ArrayList<>();


    public Song() {}

    public Song(String id, String title, String artistId, String artistName, String albumId,
                String albumName, String coverRes, String audioRes, String genre,
                String description, int durationSec, int plays, int likes) {
        this.id = id;
        this.title = title;
        this.artistId = artistId;
        this.artistName = artistName;
        this.album_id = albumId;
        this.albumName = albumName;
        this.cover_url = coverRes;
        this.audio_url = audioRes;
        this.genre = genre;
        this.description = description;
        this.duration_seconds = durationSec;
        this.plays = plays;
        this.likes = likes;
    }

    public String getStatus() { return status; }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getAudioUrl() {
        return audio_url;
    }

    public String getCoverUrl() {
        return cover_url;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public int getDurationSeconds() {
        return duration_seconds;
    }

    public String getGenre() {
        return genre;
    }

    public int getPlays() {
        return plays;
    }

    // THÊM HÀM GET LIKES ĐỂ BÊN ACTIVITY CÓ THỂ LỌC DỮ LIỆU
    public int getLikes() {
        return likes;
    }

    public void setStatus(String status) { this.status = status; }

    public void parseLrcFile(Context context, int lrcResId) {
        ArrayList<LyricLine> lyricsList = new ArrayList<>();
        try {
            InputStream is = context.getResources().openRawResource(lrcResId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;

            // Mẫu tìm kiếm chuẩn của file .lrc: [mm:ss.xx] Lời bài hát
            Pattern pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");

            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    int min = Integer.parseInt(matcher.group(1));
                    int sec = Integer.parseInt(matcher.group(2));
                    int millis = Integer.parseInt(matcher.group(3));

                    // Chuẩn hóa mili-giây (nếu file lrc chỉ ghi 2 số thì nhân 10)
                    if (matcher.group(3).length() == 2) millis *= 10;

                    int totalTimeMs = (min * 60 * 1000) + (sec * 1000) + millis;
                    String text = matcher.group(4).trim();

                    lyricsList.add(new LyricLine(totalTimeMs, text));
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.lyrics = lyricsList;
    }

    public ArrayList<LyricLine> getLyrics() {
        return lyrics;
    }

    public String getLyricsUrl() {
        return lyrics_url;
    }

    public String getAlbumId() {
        return album_id;
    }
}