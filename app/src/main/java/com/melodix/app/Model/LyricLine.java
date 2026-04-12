package com.melodix.app.Model;

import java.io.Serializable;

public class LyricLine implements Serializable {
    public long timeMs;
    public String text;

    public LyricLine() {
    }

    public LyricLine(long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text;
    }
}
