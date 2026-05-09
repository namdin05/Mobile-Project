package com.melodix.core.model;

import com.google.gson.annotations.SerializedName;

public class LyricLine {
    @SerializedName("time")
    private int time;
    @SerializedName("text")
    private String text;

    public LyricLine(int time, String text) {
        this.time = time;
        this.text = text;
    }

    public int getTime() { return time; }
    public String getText() { return text; }
}