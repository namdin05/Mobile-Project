package com.melodix.app.Utils;

import android.content.Context;
import android.content.Intent;

public class ShareUtils {
    public static void share(Context context, String title, String body) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        context.startActivity(Intent.createChooser(intent, "Share with"));
    }
}
