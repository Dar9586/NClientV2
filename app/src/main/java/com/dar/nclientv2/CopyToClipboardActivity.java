package com.dar.nclientv2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CopyToClipboardActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Uri uri = getIntent().getData();
            if (uri != null) {
                copyTextToClipboard(uri.toString());
                Toast.makeText(this, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }
            finish();
        }

        private void copyTextToClipboard(String url) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("URL", url);
            clipboard.setPrimaryClip(clip);
        }
}
