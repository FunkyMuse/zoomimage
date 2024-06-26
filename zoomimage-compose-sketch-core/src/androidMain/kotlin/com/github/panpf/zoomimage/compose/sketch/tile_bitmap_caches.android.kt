package com.github.panpf.zoomimage.compose.sketch

import com.github.panpf.sketch.Sketch
import com.github.panpf.zoomimage.sketch.SketchAndroidTileBitmapCache
import com.github.panpf.zoomimage.subsampling.TileBitmapCache

actual fun createTileBitmapCache(sketch: Sketch): TileBitmapCache =
    SketchAndroidTileBitmapCache(sketch)