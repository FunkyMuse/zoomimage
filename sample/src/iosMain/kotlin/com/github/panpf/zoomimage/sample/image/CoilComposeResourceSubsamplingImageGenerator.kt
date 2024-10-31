package com.github.panpf.zoomimage.sample.image

import androidx.compose.ui.graphics.painter.Painter
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.github.panpf.sketch.fetch.isComposeResourceUri
import com.github.panpf.sketch.util.toUri
import com.github.panpf.zoomimage.compose.coil.CoilComposeSubsamplingImageGenerator
import com.github.panpf.zoomimage.subsampling.ComposeResourceImageSource
import com.github.panpf.zoomimage.subsampling.SubsamplingImage
import com.github.panpf.zoomimage.subsampling.SubsamplingImageGenerateResult

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class CoilComposeResourceSubsamplingImageGenerator :
    CoilComposeSubsamplingImageGenerator {

    actual override suspend fun generateImage(
        context: PlatformContext,
        imageLoader: ImageLoader,
        request: ImageRequest,
        result: SuccessResult,
        painter: Painter
    ): SubsamplingImageGenerateResult? {
        val uri = when (val model = request.data) {
            is String -> model.toUri()
            is coil3.Uri -> model.toString().toUri()
            is platform.Foundation.NSURL -> model.toString().toUri()
            else -> null
        }
        if (uri != null && isComposeResourceUri(uri)) {
            val resourcePath = uri.pathSegments.drop(1).joinToString("/")
            val imageSource = ComposeResourceImageSource.Factory(resourcePath)
            return SubsamplingImageGenerateResult.Success(SubsamplingImage(imageSource, null))
        }
        return null
    }
}