package com.github.panpf.zoomimage.sample.ui.examples

import androidx.compose.runtime.Composable
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.cacheDecodeTimeoutFrame

@Composable
actual fun ImageRequest.Builder.SketchPhotoGridItemImageConfig() {
    cacheDecodeTimeoutFrame()
}