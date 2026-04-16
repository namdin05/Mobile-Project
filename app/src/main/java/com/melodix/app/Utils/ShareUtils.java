package com.melodix.app.Utils;

import android.content.Context;
import android.content.Intent;

import com.melodix.app.Model.Song;

public class ShareUtils {
    public static void share(Context context, String title, String body) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        context.startActivity(Intent.createChooser(intent, "Share with"));
    }

    public static void shareSongToFriends(Context context, Song song) {
        if (song == null) return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String shareMessage = "🎵 Mình đang nghe bài '" + song.getTitle() + "' cực cuốn!\n"
                + "👉 Bấm vào đây để nghe cùng trên Melodix: \n"
                + "https://giabaocode.github.io/melodix-redirect/?id=" + song.getId();

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        // Dùng 'context' thay vì 'this'
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ bài hát qua..."));
    }

    // Hàm đa năng: Share User, Share Playlist, Share Album,...
    public static void shareContent(android.content.Context context, String type, String id, String title) {
        // Lấy thời gian hiện tại (tính bằng mili-giây)
        long timeStamp = System.currentTimeMillis();

        // Nhét thêm cái &t=... vào cuối link. Mỗi lần Share là 1 con số khác nhau!
        String link = "https://giabaocode.github.io/melodix-redirect/?type=" + type + "&id=" + id + "&t=" + timeStamp;

        String message = "Khám phá \"" + title + "\" trên Melodix ngay!\n\nNhấn vào link để xem: " + link;

        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Chia sẻ từ Melodix");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);

        context.startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ qua"));
    }
}
