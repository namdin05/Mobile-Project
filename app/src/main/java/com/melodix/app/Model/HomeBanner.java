package com.melodix.app.Model;

import com.google.firebase.firestore.DocumentSnapshot;

import java.io.Serializable;

public class HomeBanner implements Serializable {

    private String id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String targetType;
    private String targetId;
    private long order;
    private boolean active;

    public HomeBanner() {
    }

    public static HomeBanner fromDocument(DocumentSnapshot document) {
        HomeBanner banner = new HomeBanner();

        if (document == null || !document.exists()) {
            return banner;
        }

        try {
            banner.setId(document.getId());
            banner.setTitle(getSafeString(document, "title"));
            banner.setSubtitle(getSafeString(document, "subtitle"));
            banner.setImageUrl(getSafeString(document, "imageUrl"));
            banner.setTargetType(getSafeString(document, "targetType"));
            banner.setTargetId(getSafeString(document, "targetId"));
            banner.setOrder(getSafeLong(document.get("order")));

            Boolean activeValue = document.getBoolean("active");
            banner.setActive(activeValue == null || activeValue);
        } catch (Exception ignored) {
        }

        return banner;
    }

    private static String getSafeString(DocumentSnapshot document, String key) {
        try {
            String value = document.getString(key);
            return value == null ? "" : value;
        } catch (Exception e) {
            return "";
        }
    }

    private static long getSafeLong(Object value) {
        try {
            if (value == null) {
                return 0L;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }

    public String getDisplayTitle() {
        if (title != null && !title.trim().isEmpty()) {
            return title;
        }
        return "Featured";
    }

    public String getDisplaySubtitle() {
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            return subtitle;
        }
        return "Khám phá nội dung nổi bật trên Melodix.";
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

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle == null ? "" : subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl == null ? "" : imageUrl;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType == null ? "" : targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId == null ? "" : targetId;
    }

    public long getOrder() {
        return order;
    }

    public void setOrder(long order) {
        this.order = Math.max(order, 0L);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}