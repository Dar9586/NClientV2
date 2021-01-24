package com.dar.nclientv2.components.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dar.nclientv2.settings.Global;
import com.dar.nclientv2.utility.LogUtility;

import java.util.ArrayList;
import java.util.List;

public class CustomWebView extends WebView {
    private final MyJavaScriptInterface javaScriptInterface;

    public CustomWebView(Context context) {
        super(context.getApplicationContext());
        javaScriptInterface = new MyJavaScriptInterface(context.getApplicationContext());
        initialize();
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        javaScriptInterface = new MyJavaScriptInterface(context.getApplicationContext());
        initialize();
    }

    public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        javaScriptInterface = new MyJavaScriptInterface(context.getApplicationContext());
        initialize();
    }

    @Override
    public void loadUrl(String url) {
        LogUtility.d("Loading url: " + url);
        super.loadUrl(url);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    //it only uses showHtml and Nhentai should be trusted if you use this app (I think)
    private void initialize() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setUserAgentString(Global.getUserAgent());
        addJavascriptInterface(javaScriptInterface, "HtmlViewer");
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                LogUtility.d("Started url: " + url);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                String html = "javascript:window.HtmlViewer.showHTML" +
                    "('" + url + "','<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');";
                loadUrl(html);
            }

        });

        addFetcher((url, html) -> LogUtility.d("Fetch for url " + url + ": " + html));
    }

    public void addFetcher(@Nullable HtmlFetcher fetcher) {
        if (fetcher == null) return;
        javaScriptInterface.addFetcher(fetcher);
    }

    public void removeFetcher(@Nullable HtmlFetcher fetcher) {
        if (fetcher == null) return;
        javaScriptInterface.removeFetcher(fetcher);
    }

    public interface HtmlFetcher {
        void fetchUrl(String url, String html);
    }

    static class MyJavaScriptInterface {
        List<HtmlFetcher> fetchers = new ArrayList<>(5);
        Context ctx;

        MyJavaScriptInterface(Context ctx) {
            this.ctx = ctx;
        }

        public void addFetcher(@NonNull HtmlFetcher fetcher) {
            fetchers.add(fetcher);
        }

        public void removeFetcher(@NonNull HtmlFetcher fetcher) {
            fetchers.remove(fetcher);
        }

        @JavascriptInterface
        public void showHTML(String url, String html) {
            for (HtmlFetcher fetcher : fetchers)
                fetcher.fetchUrl(url, html);
        }
    }


}
