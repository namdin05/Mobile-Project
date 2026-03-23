package com.melodix.app.Model;

import com.google.firebase.firestore.DocumentSnapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SongLyrics implements Serializable {

    private String songId;
    private String plainLyrics;
    private List<LyricLine> syncedLines;
    private boolean active;

    public SongLyrics() {
        songId = "";
        plainLyrics = "";
        syncedLines = new ArrayList<>();
        active = true;
    }

    public static SongLyrics fromDocument(DocumentSnapshot document) {
        SongLyrics lyrics = new SongLyrics();

        if (document == null || !document.exists()) {
            return lyrics;
        }

        try {
            lyrics.setSongId(document.getId());

            String explicitSongId = getSafeString(document, "songId");
            if (!explicitSongId.isEmpty()) {
                lyrics.setSongId(explicitSongId);
            }

            String plainLyrics = getSafeString(document, "plainLyrics");
            if (plainLyrics.isEmpty()) {
                plainLyrics = getSafeString(document, "lyrics");
            }
            if (plainLyrics.isEmpty()) {
                plainLyrics = getSafeString(document, "plainText");
            }
            if (plainLyrics.isEmpty()) {
                plainLyrics = getSafeString(document, "text");
            }
            lyrics.setPlainLyrics(plainLyrics);

            Object syncedRaw = document.get("syncedLines");
            if (syncedRaw == null) {
                syncedRaw = document.get("lines");
            }
            if (syncedRaw == null) {
                syncedRaw = document.get("lyricsLines");
            }

            lyrics.setSyncedLines(parseSyncedLines(syncedRaw));

            Boolean activeValue = document.getBoolean("active");
            lyrics.setActive(activeValue == null || activeValue);
        } catch (Exception ignored) {
        }

        return lyrics;
    }

    private static String getSafeString(DocumentSnapshot document, String key) {
        try {
            String value = document.getString(key);
            return value == null ? "" : value.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static List<LyricLine> parseSyncedLines(Object rawValue) {
        List<LyricLine> lines = new ArrayList<>();

        try {
            if (rawValue instanceof List<?>) {
                List<?> rawList = (List<?>) rawValue;
                for (Object item : rawList) {
                    LyricLine line = LyricLine.fromMap(item);
                    if (line != null && !line.getDisplayText().isEmpty()) {
                        lines.add(line);
                    }
                }
            }

            Collections.sort(lines, new Comparator<LyricLine>() {
                @Override
                public int compare(LyricLine first, LyricLine second) {
                    return Long.compare(first.getTimeMs(), second.getTimeMs());
                }
            });
        } catch (Exception ignored) {
        }

        return lines;
    }

    public boolean hasSyncedLyrics() {
        return syncedLines != null && !syncedLines.isEmpty();
    }

    public boolean hasPlainLyrics() {
        return plainLyrics != null && !plainLyrics.trim().isEmpty();
    }

    public boolean hasAnyLyrics() {
        return hasSyncedLyrics() || hasPlainLyrics();
    }

    public List<LyricLine> getDisplayLines() {
        List<LyricLine> lines = new ArrayList<>();

        try {
            if (hasSyncedLyrics()) {
                lines.addAll(getSyncedLines());
                return lines;
            }

            if (!hasPlainLyrics()) {
                return lines;
            }

            String normalized = getPlainLyrics().replace("\r\n", "\n").replace("\r", "\n");
            String[] rawLines = normalized.split("\n", -1);

            for (String rawLine : rawLines) {
                String safeText = rawLine == null ? "" : rawLine;
                lines.add(new LyricLine(-1L, safeText));
            }
        } catch (Exception ignored) {
        }

        return lines;
    }

    public int getActiveSyncedLineIndex(long currentPositionMs) {
        try {
            if (!hasSyncedLyrics()) {
                return -1;
            }

            int activeIndex = -1;
            List<LyricLine> lines = getSyncedLines();

            for (int i = 0; i < lines.size(); i++) {
                LyricLine line = lines.get(i);
                if (line == null) {
                    continue;
                }

                if (currentPositionMs >= line.getTimeMs()) {
                    activeIndex = i;
                } else {
                    break;
                }
            }

            return activeIndex;
        } catch (Exception e) {
            return -1;
        }
    }

    public String getPreviewText(long currentPositionMs) {
        try {
            if (hasSyncedLyrics()) {
                int activeIndex = getActiveSyncedLineIndex(currentPositionMs);
                if (activeIndex < 0) {
                    activeIndex = getFirstNonEmptySyncedLineIndex();
                }

                if (activeIndex >= 0 && activeIndex < getSyncedLines().size()) {
                    return getSyncedLines().get(activeIndex).getDisplayText();
                }
            }

            if (hasPlainLyrics()) {
                String normalized = getPlainLyrics().replace("\r\n", "\n").replace("\r", "\n");
                String[] rawLines = normalized.split("\n", -1);

                StringBuilder builder = new StringBuilder();
                int added = 0;

                for (String rawLine : rawLines) {
                    String safeText = rawLine == null ? "" : rawLine.trim();
                    if (safeText.isEmpty()) {
                        continue;
                    }

                    if (builder.length() > 0) {
                        builder.append("\n");
                    }

                    builder.append(safeText);
                    added++;

                    if (added >= 3) {
                        break;
                    }
                }

                return builder.toString();
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    private int getFirstNonEmptySyncedLineIndex() {
        try {
            List<LyricLine> lines = getSyncedLines();
            for (int i = 0; i < lines.size(); i++) {
                LyricLine line = lines.get(i);
                if (line != null && !line.getDisplayText().trim().isEmpty()) {
                    return i;
                }
            }
        } catch (Exception ignored) {
        }

        return -1;
    }

    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId == null ? "" : songId;
    }

    public String getPlainLyrics() {
        return plainLyrics;
    }

    public void setPlainLyrics(String plainLyrics) {
        this.plainLyrics = plainLyrics == null ? "" : plainLyrics;
    }

    public List<LyricLine> getSyncedLines() {
        return syncedLines == null ? new ArrayList<>() : syncedLines;
    }

    public void setSyncedLines(List<LyricLine> syncedLines) {
        this.syncedLines = syncedLines == null ? new ArrayList<>() : syncedLines;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}