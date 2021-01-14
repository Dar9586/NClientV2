package com.dar.nclientv2.components.views;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dar.nclientv2.R;
import com.dar.nclientv2.api.components.Gallery;
import com.dar.nclientv2.async.downloader.DownloadGalleryV2;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class RangeSelector extends MaterialAlertDialogBuilder {
    private final Gallery gallery;
    private SeekBar s1, s2;

    public RangeSelector(@NonNull Context context, Gallery gallery) {
        super(context);
        this.gallery = gallery;
        View v = View.inflate(context, R.layout.range_selector, null);
        setView(v);
        LinearLayout l1 = v.findViewById(R.id.layout1);
        LinearLayout l2 = v.findViewById(R.id.layout2);
        applyLogic(l1, true);
        applyLogic(l2, false);
        setPositiveButton(R.string.ok, (dialog, which) -> {
            if (s1.getProgress() <= s2.getProgress())
                DownloadGalleryV2.downloadRange(context, gallery, s1.getProgress(), s2.getProgress());
            else
                Toast.makeText(context, R.string.invalid_range_selected, Toast.LENGTH_SHORT).show();
        }).setNegativeButton(R.string.cancel, null);
        setCancelable(true);
    }

    private View.OnClickListener getPrevListener(SeekBar s) {
        return v -> s.setProgress(s.getProgress() - 1);
    }

    private View.OnClickListener getNextListener(SeekBar s) {
        return v -> s.setProgress(s.getProgress() + 1);
    }

    private SeekBar.OnSeekBarChangeListener getSeekBarListener(TextView t) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                t.setText(getContext().getString(R.string.page_format, progress + 1, gallery.getPageCount()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
    }

    private void applyLogic(LinearLayout layout, boolean start) {
        ImageButton prev = layout.findViewById(R.id.prev);
        ImageButton next = layout.findViewById(R.id.next);
        TextView pages = layout.findViewById(R.id.pages);
        SeekBar seekBar = layout.findViewById(R.id.seekBar);
        prev.setOnClickListener(getPrevListener(seekBar));
        next.setOnClickListener(getNextListener(seekBar));
        seekBar.setMax(gallery.getPageCount() - 1);
        seekBar.setOnSeekBarChangeListener(getSeekBarListener(pages));
        seekBar.setProgress(1);
        seekBar.setProgress(start ? 0 : gallery.getPageCount());
        if (start) s1 = seekBar;
        else s2 = seekBar;
    }
}
