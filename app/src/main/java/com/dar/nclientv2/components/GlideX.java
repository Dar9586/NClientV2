package com.dar.nclientv2.components;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

public class GlideX {
    @Nullable
    public static Glide get(Context context) {
        try {
            return Glide.get(context);
        } catch (VerifyError | IllegalStateException ignore) {
            return null;
        }
    }

    @Nullable
    public static RequestManager with(View view) {
        try {
            return Glide.with(view);
        } catch (VerifyError | IllegalStateException ignore) {
            return null;
        }
    }

    @Nullable
    public static RequestManager with(Context context) {
        try {
            return Glide.with(context);
        } catch (VerifyError | IllegalStateException ignore) {
            return null;
        }
    }

    @Nullable
    public static RequestManager with(Fragment fragment) {
        try {
            return Glide.with(fragment);
        } catch (VerifyError | IllegalStateException ignore) {
            return null;
        }
    }

    @Nullable
    public static RequestManager with(FragmentActivity fragmentActivity) {
        try {
            return Glide.with(fragmentActivity);
        } catch (VerifyError | IllegalStateException ignore) {
            return null;
        }
    }

    @Nullable
    public static RequestManager with(Activity activity) {
        try {
            return Glide.with(activity);
        } catch (VerifyError | IllegalStateException ignore) {
            return null;
        }
    }
}
