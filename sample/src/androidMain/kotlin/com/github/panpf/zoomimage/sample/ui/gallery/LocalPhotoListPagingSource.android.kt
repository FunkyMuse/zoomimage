package com.github.panpf.zoomimage.sample.ui.gallery

import android.graphics.RectF
import android.provider.MediaStore.Images.Media
import androidx.core.content.PermissionChecker
import com.caverock.androidsvg.SVG
import com.githb.panpf.zoomimage.images.AndroidResourceImages
import com.githb.panpf.zoomimage.images.ContentImages
import com.githb.panpf.zoomimage.images.HttpImages
import com.githb.panpf.zoomimage.images.ImageFile
import com.githb.panpf.zoomimage.images.LocalImages
import com.githb.panpf.zoomimage.images.ResourceImages
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.decode.SvgDecoder
import com.github.panpf.sketch.decode.internal.AndroidExifOrientationHelper
import com.github.panpf.sketch.decode.internal.readExifOrientation
import com.github.panpf.sketch.decode.internal.readImageInfoWithBitmapFactoryOrThrow
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.tools4k.coroutines.withToIO
import com.github.panpf.zoomimage.sample.ComposeResourceImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer

actual fun builtinImages(): List<ImageFile> {
    return listOf(
        ResourceImages.cat,
        ResourceImages.dog,
        ResourceImages.anim,
        ResourceImages.longEnd,
        ContentImages.longWhale,
        ComposeResourceImages.hugeChina,
        AndroidResourceImages.hugeCard,
        LocalImages.hugeLongQmsht,
        HttpImages.hugeLongComic,
    )
}

actual suspend fun readPhotosFromPhotoAlbum(
    context: PlatformContext,
    startPosition: Int,
    pageSize: Int
): List<String> {
    val checkSelfPermission = PermissionChecker
        .checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
    if (checkSelfPermission != PermissionChecker.PERMISSION_GRANTED) {
        return emptyList()
    }
    return withToIO {
        val cursor = context.contentResolver.query(
            /* uri = */ Media.EXTERNAL_CONTENT_URI,
            /* projection = */
            arrayOf(
                Media.TITLE,
                Media.DATA,
                Media.SIZE,
                Media.DATE_TAKEN,
            ),
            /* selection = */
            null,
            /* selectionArgs = */
            null,
            /* sortOrder = */
            Media.DATE_TAKEN + " DESC" + " limit " + startPosition + "," + pageSize
        )
        ArrayList<String>(cursor?.count ?: 0).apply {
            cursor?.use {
                while (cursor.moveToNext()) {
                    val uri =
                        cursor.getString(cursor.getColumnIndexOrThrow(Media.DATA))
                    add(uri)
                }
            }
        }
    }
}

actual suspend fun readImageInfoOrNull(
    context: PlatformContext,
    sketch: Sketch,
    uri: String,
): ImageInfo? = withContext(Dispatchers.IO) {
    runCatching {
        val fetcher = sketch.components.newFetcherOrThrow(ImageRequest(context, uri))
        val dataSource = fetcher.fetch().getOrThrow().dataSource
        if (uri.endsWith(".svg")) {
            dataSource.readSVGImageInfo()
        } else {
            val imageInfo = dataSource.readImageInfoWithBitmapFactoryOrThrow()
            val exifOrientation = dataSource.readExifOrientation()
            val exifOrientationHelper = AndroidExifOrientationHelper(exifOrientation)
            val newSize = exifOrientationHelper.applyToSize(imageInfo.size)
            imageInfo.copy(size = newSize)
        }
    }.apply {
        if (isFailure) {
            exceptionOrNull()?.printStackTrace()
        }
    }.getOrNull()
}

private fun DataSource.readSVGImageInfo(useViewBoundsAsIntrinsicSize: Boolean = true): ImageInfo {
    val svg = openSource().buffer().inputStream().use { SVG.getFromInputStream(it) }
    val width: Int
    val height: Int
    val viewBox: RectF? = svg.documentViewBox
    if (useViewBoundsAsIntrinsicSize && viewBox != null) {
        width = viewBox.width().toInt()
        height = viewBox.height().toInt()
    } else {
        width = svg.documentWidth.toInt()
        height = svg.documentHeight.toInt()
    }
    return ImageInfo(width = width, height = height, mimeType = SvgDecoder.MIME_TYPE)
}
