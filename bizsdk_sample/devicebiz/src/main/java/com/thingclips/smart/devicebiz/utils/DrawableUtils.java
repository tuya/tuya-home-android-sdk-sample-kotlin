package com.thingclips.smart.devicebiz.utils;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;

public class DrawableUtils {
    public static Drawable tintDrawable(@NonNull Drawable drawable, @ColorInt int tintColor) {
        return tintListDrawable(drawable, ColorStateList.valueOf(tintColor), PorterDuff.Mode.SRC_IN);
    }

    public static Drawable tintListDrawable(@NonNull Drawable drawable, @Nullable ColorStateList tintColors, @NonNull PorterDuff.Mode tintMode) {
        // 获取此drawable的共享状态实例
        Drawable wrappedD = DrawableCompat.wrap(drawable).mutate();
        DrawableCompat.setTintMode(wrappedD, tintMode);
        DrawableCompat.setTintList(wrappedD, tintColors);
        return wrappedD;
    }

}
