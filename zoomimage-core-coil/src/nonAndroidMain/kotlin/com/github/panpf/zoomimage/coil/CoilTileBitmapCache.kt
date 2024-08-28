/*
 * Copyright (C) 2024 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.panpf.zoomimage.coil

import coil3.BitmapImage
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.memory.MemoryCache
import com.github.panpf.zoomimage.subsampling.BitmapFrom
import com.github.panpf.zoomimage.subsampling.ImageInfo
import com.github.panpf.zoomimage.subsampling.SkiaTileBitmap
import com.github.panpf.zoomimage.subsampling.TileBitmap
import com.github.panpf.zoomimage.subsampling.TileBitmapCache

/**
 * Implement [TileBitmapCache] based on Coil on Non Android platform
 *
 * @see com.github.panpf.zoomimage.core.coil.nonandroid.test.CoilTileBitmapCacheTest
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class CoilTileBitmapCache actual constructor(
    private val imageLoader: ImageLoader
) : TileBitmapCache {

    @OptIn(ExperimentalCoilApi::class)
    actual override fun get(key: String): TileBitmap? {
        val cacheValue = imageLoader.memoryCache?.get(MemoryCache.Key(key)) ?: return null
        val image = cacheValue.image as BitmapImage
        return SkiaTileBitmap(image.bitmap, key, BitmapFrom.MEMORY_CACHE)
    }

    @OptIn(ExperimentalCoilApi::class)
    actual override fun put(
        key: String,
        tileBitmap: TileBitmap,
        imageUrl: String,
        imageInfo: ImageInfo,
    ): TileBitmap? {
        tileBitmap as SkiaTileBitmap
        val bitmap = tileBitmap.bitmap
        val memoryCache = imageLoader.memoryCache
        memoryCache?.set(MemoryCache.Key(key), MemoryCache.Value(bitmap.asImage()))
        return null
    }
}