package com.dar.nclientv2.components.views;

import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

import com.dar.nclientv2.R;
import com.dar.nclientv2.components.CookieInterceptor;

public class CFTokenView {

    private final ViewGroup masterLayout;
    private WebView webView;
    private Button button;

    public CFTokenView(ViewGroup masterLayout) {
        this.masterLayout = masterLayout;
        webView=masterLayout.findViewById(R.id.webView);
        button=masterLayout.findViewById(R.id.hideWebView);
        button.setOnClickListener(v -> CookieInterceptor.hideWebView());
    }

    public Button getButton() {
        return button;
    }

    public WebView getWebView() {
        return webView;
    }

    public void setVisibility(int visible) {
        masterLayout.setVisibility(visible);
    }

    public void post(Runnable o) {
        masterLayout.post(o);
    }
}
