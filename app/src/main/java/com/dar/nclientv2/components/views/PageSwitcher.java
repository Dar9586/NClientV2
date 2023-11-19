package com.dar.nclientv2.components.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.cardview.widget.CardView;

import com.dar.nclientv2.R;
import com.dar.nclientv2.settings.DefaultDialogs;

import java.util.Locale;

public class PageSwitcher extends CardView {

    private LinearLayout master;
    private AppCompatImageButton prev, next;
    private AppCompatEditText text;
    @Nullable
    private PageChanger changer;
    private int totalPage;
    private int actualPage;

    public PageSwitcher(@NonNull Context context) {
        super(context);
        init(context);
    }


    public PageSwitcher(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
        setPages(0, 0);
    }

    public PageSwitcher(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setChanger(@Nullable PageChanger changer) {
        this.changer = changer;
    }

    public void setPages(int totalPage, int actualPage) {
        actualPage = Math.min(totalPage, Math.max(actualPage, 1));
        boolean pageChanged = this.actualPage != actualPage;
        if (this.totalPage == totalPage && !pageChanged) return;
        this.totalPage = totalPage;
        this.actualPage = actualPage;
        if (pageChanged && changer != null) changer.pageChanged(this, actualPage);
        updateViews();
    }

    public void setTotalPage(int totalPage) {
        setPages(totalPage, actualPage);
    }

    private void updateViews() {
        ((Activity) getContext()).runOnUiThread(() -> {
            setVisibility(totalPage <= 1 ? View.GONE : View.VISIBLE);
            prev.setAlpha(actualPage > 1 ? 1f : .5f);
            prev.setEnabled(actualPage > 1);
            next.setAlpha(actualPage < totalPage ? 1f : .5f);
            next.setEnabled(actualPage < totalPage);
            text.setText(String.format(Locale.US, "%d / %d", actualPage, totalPage));
        });
    }

    private void init(Context context) {
        master = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.page_switcher, this, true).findViewById(R.id.master_layout);
        prev = master.findViewById(R.id.prev);
        next = master.findViewById(R.id.next);
        text = master.findViewById(R.id.page_index);
        addViewListeners();
    }

    private void addViewListeners() {
        next.setOnClickListener(v -> {
            if (changer != null) changer.onNextClicked(this);
        });
        prev.setOnClickListener(v -> {
            if (changer != null) changer.onPrevClicked(this);
        });
        text.setOnClickListener(v -> loadDialog());
    }

    public int getActualPage() {
        return actualPage;
    }

    public void setActualPage(int actualPage) {
        setPages(totalPage, actualPage);
    }

    public boolean lastPageReached() {
        return actualPage == totalPage;
    }

    private void loadDialog() {
        DefaultDialogs.pageChangerDialog(
            new DefaultDialogs.Builder(getContext())
                .setActual(actualPage)
                .setMin(1)
                .setMax(totalPage)
                .setTitle(R.string.change_page)
                .setDrawable(R.drawable.ic_find_in_page)
                .setDialogs(new DefaultDialogs.CustomDialogResults() {
                    @Override
                    public void positive(int actual) {
                        setActualPage(actual);
                    }
                })
        );
    }


    public interface PageChanger {
        void pageChanged(PageSwitcher switcher, int page);

        void onPrevClicked(PageSwitcher switcher);

        void onNextClicked(PageSwitcher switcher);
    }

    public static class DefaultPageChanger implements PageChanger {

        @Override
        public void pageChanged(PageSwitcher switcher, int page) {
        }

        @Override
        public void onPrevClicked(PageSwitcher switcher) {
            switcher.setActualPage(switcher.getActualPage() - 1);
        }

        @Override
        public void onNextClicked(PageSwitcher switcher) {
            switcher.setActualPage(switcher.getActualPage() + 1);
        }
    }
}
