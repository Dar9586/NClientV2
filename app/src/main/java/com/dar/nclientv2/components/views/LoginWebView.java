package com.dar.nclientv2.components.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import com.dar.nclientv2.utility.LogUtility;
import com.dar.nclientv2.utility.Utility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class LoginWebView extends CustomWebView {
    private HtmlFetcher fetcher=new HtmlFetcher() {
        @Override
        public void fetchUrl(String url, String html) {
            Document jsoup=Jsoup.parse(html);
            Element body=jsoup.body();
            Element form=body.getElementsByTag("form").first();
            body.getElementsByClass("lead").first().text("Tested");
            form.tagName("div");
            form.before("<script>\n" +
                    "document.getElementsByClassName('lead')[0].innerHTML='test';\n"+
                    "alert('test');\n"+
                    "function intercept(){\n" +
                    "    password=document.getElementById('id_password').value;\n" +
                    "    email=document.getElementById('id_username_or_email').value;\n" +
                    "    token=document.getElementsByName('csrfmiddlewaretoken')[0].value;\n" +
                    "    captcha=document.getElementById('g-recaptcha-response').value;\n" +
                    "     Interceptor.intercept(email,password,token,captcha);\n" +
                    "}\n" +
                    "</script>");
            form.getElementsByAttributeValue("type","submit").first().attr("onclick","intercept()");
            removeFetcher(fetcher);
            String encodedHtml = Base64.encodeToString(jsoup.outerHtml().getBytes(), Base64.NO_PADDING);
            loadDataWithBaseURL(Utility.getBaseUrl(), encodedHtml,"text/html","base64",null);
        }
    };
    public LoginWebView(Context context) {
        super(context);
        init(context);
    }

    public LoginWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoginWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressLint("AddJavascriptInterface")
    private void init(Context context) {
        addJavascriptInterface(new JSInterceptor(),"Interceptor");
        addFetcher(fetcher);
        loadUrl(Utility.getBaseUrl()+"login/");
    }
    static class JSInterceptor{
        @JavascriptInterface
        public void intercept(String email,String password,String token,String captcha){
            LogUtility.d(String.format("e:'%s',p:'%s',t:'%s',c:'%s'",email,password,token,captcha));
            return;
        }
    }
}
