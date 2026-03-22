package com.melodix.app.Model;

public class SearchResultItem {
    public static final int TYPE_SONG = 1;
    public static final int TYPE_ARTIST = 2;

    public static final int TYPE_ALBUM = 3;

    private final String id;
    private final int type;
    private final String title;
    private final String subtitle;
    private final String imageUrl;

    private final String artistId;
    private final String albumId;

    public SearchResultItem(String id, int type, String title, String subtitle, String imageUrl, String artistId, String albumId) {
        this.id = id == null ? "" : id;
        this.type = type;
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.artistId = artistId == null ? "" : artistId;
        this.albumId = albumId == null ? "" : albumId;
    }

    public SearchResultItem(String id, int type, String title, String subtitle, String imageUrl) {
        this(id, type, title, subtitle, imageUrl, "", "");
    }

    public String getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getArtistId() {
        return artistId;
    }

    public String getAlbumId() {
        return albumId;
    }
}