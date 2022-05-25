package com.dar.nclientv2.components;

import androidx.annotation.NonNull;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.cache.CookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class CustomCookieJar implements ClearableCookieJar {
    private final CookieCache cache;
    private final CookiePersistor persistor;

    public CustomCookieJar(CookieCache cache, CookiePersistor persistor) {
        this.cache = cache;
        this.persistor = persistor;

        this.cache.addAll(persistor.loadAll());
    }

    private static List<Cookie> filterPersistentCookies(List<Cookie> cookies) {
        List<Cookie> persistentCookies = new ArrayList<>();

        for (Cookie cookie : cookies) {
            if (cookie.persistent()) {
                persistentCookies.add(cookie);
            }
        }
        return persistentCookies;
    }

    private static boolean isCookieExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    @Override
    synchronized public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        cache.addAll(cookies);
        persistor.saveAll(filterPersistentCookies(cookies));
    }

    @NonNull
    @Override
    synchronized public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        List<Cookie> cookiesToRemove = new ArrayList<>();
        List<Cookie> validCookies = new ArrayList<>();

        for (Iterator<Cookie> it = cache.iterator(); it.hasNext(); ) {
            Cookie currentCookie = it.next();

            if (isCookieExpired(currentCookie)) {
                cookiesToRemove.add(currentCookie);
                it.remove();

            } else {
                validCookies.add(currentCookie);
            }
        }

        persistor.removeAll(cookiesToRemove);
        return validCookies;
    }

    @Override
    synchronized public void clearSession() {
        cache.clear();
        cache.addAll(persistor.loadAll());
    }

    @Override
    synchronized public void clear() {
        cache.clear();
        persistor.clear();
    }

    public void removeCookie(String name) {
        List<Cookie> cookies = persistor.loadAll();
        for (Cookie cookie : cookies) {
            if (cookie.name().equals(name)) {
                cache.clear();
                persistor.removeAll(Collections.singletonList(cookie));
            }
        }
    }
}
