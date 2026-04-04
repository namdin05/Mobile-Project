package com.melodix.app.Model;

import java.io.Serializable;

public class SearchResultItem implements Serializable {
    public String type;
    public String targetId;
    public String title;
    public String subtitle;
    public String coverRes;

    public SearchResultItem() {
    }

    public SearchResultItem(String type, String targetId, String title, String subtitle, String coverRes) {
        this.type = type;
        this.targetId = targetId;
        this.title = title;
        this.subtitle = subtitle;
        this.coverRes = coverRes;
    }
}
