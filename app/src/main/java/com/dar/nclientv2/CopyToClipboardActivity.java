package com.dar.nclientv2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.dar.nclientv2.components.activities.GeneralActivity;

public class CopyToClipboardActivity extends GeneralActivity {
    public static void copyTextToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text", text);
        if (clipboard != null)
            clipboard.setPrimaryClip(clip);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        if (uri != null) {
            copyTextToClipboard(this, uri.toString());
            Toast.makeText(this, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
