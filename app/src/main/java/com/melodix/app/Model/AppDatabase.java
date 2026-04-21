package com.melodix.app.Model;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                DownloadedSong.class,
                // Thêm các entity khác của ở đây
        },
        version = 2,   // Tăng version nếu đã có database cũ
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract DownloadedSongDao downloadedSongDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "melodix_database")
                            .fallbackToDestructiveMigration()   // Xóa database khi thay đổi schema (dev)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}