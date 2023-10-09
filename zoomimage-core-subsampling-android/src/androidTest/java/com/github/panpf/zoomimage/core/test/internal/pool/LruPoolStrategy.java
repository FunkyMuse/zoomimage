package com.github.panpf.zoomimage.core.test.internal.pool;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface LruPoolStrategy {

    void put(Bitmap bitmap);

    @Nullable
    Bitmap get(int width, int height, Bitmap.Config config);

    boolean exist(int width, int height, Bitmap.Config config);

    @Nullable
    Bitmap removeLast();

    @NonNull
    String logBitmap(@NonNull Bitmap bitmap);

    @NonNull
    String logBitmap(int width, int height, @Nullable Bitmap.Config config);

    int getSize(@NonNull Bitmap bitmap);
}
