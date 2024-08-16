package com.github.panpf.zoomimage.images.coil

import coil3.Uri
import coil3.pathSegments
import com.github.panpf.zoomimage.coil.CoilModelToImageSource
import kotlinx.collections.immutable.ImmutableList

/**
 * Check if the uri is a Kotlin resource uri
 *
 * Sample: 'file:///kotlin_resource/test.png'
 */
fun isKotlinResourceUri(uri: Uri): Boolean =
    "file".equals(uri.scheme, ignoreCase = true)
            && uri.authority?.takeIf { it.isNotEmpty() } == null
            && "kotlin_resource"
        .equals(uri.pathSegments.firstOrNull(), ignoreCase = true)

expect fun platformModeToImageSources(): ImmutableList<CoilModelToImageSource>