/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
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
package com.github.panpf.zoom.internal

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import androidx.annotation.MainThread
import androidx.exifinterface.media.ExifInterface
import com.github.panpf.zoom.ImageSource
import com.github.panpf.zoom.Logger
import com.github.panpf.zoom.OnTileChangedListener
import com.github.panpf.zoom.Size
import com.github.panpf.zoom.TinyBitmapPool
import com.github.panpf.zoom.TinyMemoryCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class SubsamplingEngine constructor(
    val context: Context,
    val logger: Logger,
    val zoomEngine: ZoomEngine,
) {

    companion object {
        internal const val MODULE = "SubsamplingEngine"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var initJob: Job? = null
    private var imageSource: ImageSource? = null
    private var tileManager: TileManager? = null
    private val tempDrawMatrix = Matrix()
    private val tempDrawableVisibleRect = Rect()
    internal var onTileChangedListenerList: MutableSet<OnTileChangedListener>? = null

    var disallowMemoryCache: Boolean = false
    var disallowReuseBitmap: Boolean = false
    var tinyBitmapPool: TinyBitmapPool? = null
    var tinyMemoryCache: TinyMemoryCache? = null
    var showTileBounds = false
        set(value) {
            if (field != value) {
                field = value
                invalidateView()
            }
        }
    var paused = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    imageSource?.run { logger.d(MODULE) { "pause. '$key'" } }
                    tileManager?.clean()
                } else {
                    imageSource?.run { logger.d(MODULE) { "resume. '$key'" } }
                    refreshTiles()
                }
            }
        }

    val destroyed: Boolean
        get() = imageSource == null
    val tileList: List<Tile>?
        get() = tileManager?.tileList

    init {
        zoomEngine.addOnMatrixChangeListener {
            refreshTiles()
        }
    }

    fun setImageSource(imageSource: ImageSource) {
        val viewSize = zoomEngine.viewSize
        if (viewSize.isEmpty) {
            logger.d(MODULE) { "setImageSource failed. View size error. '${imageSource.key}'" }
            return
        }
        initJob?.cancel("setImageSource")
        initJob = scope.launch(Dispatchers.Main) {
            val optionsJob = async(Dispatchers.IO) {
                kotlin.runCatching {
                    imageSource.openInputStream()
                        .let { it.getOrNull() ?: throw it.exceptionOrNull()!! }
                        .use { inputStream ->
                            BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                                BitmapFactory.decodeStream(inputStream, null, this)
                            }
                        }.takeIf { it.outWidth > 0 && it.outHeight > 0 }
                }
            }
            val orientationUndefined = ExifInterface.ORIENTATION_UNDEFINED
            val exifOrientationJob = async(Dispatchers.IO) {
                kotlin.runCatching {
                    imageSource.openInputStream()
                        .let { it.getOrNull() ?: throw it.exceptionOrNull()!! }
                        .use { inputStream ->
                            ExifInterface(inputStream)
                                .getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION,
                                    orientationUndefined
                                )
                        }
                }
            }
            val options = optionsJob.await()
                .apply { exceptionOrNull()?.printStackTrace() }
                .getOrNull()
            if (options == null) {
                logger.d(MODULE) { "setImageSource failed. Can't decode image bounds. '${imageSource.key}'" }
                return@launch
            }
            val exifOrientation = exifOrientationJob.await()
                .apply { exceptionOrNull()?.printStackTrace() }
                .getOrNull()
            if (exifOrientation == null) {
                logger.d(MODULE) { "setImageSource failed. Can't decode image exifOrientation. '${imageSource.key}'" }
                return@launch
            }
            val imageWidth = options.outWidth
            val imageHeight = options.outHeight
            val imageType = options.outMimeType
            this@SubsamplingEngine.imageSource = imageSource
            val imageSize = Size(imageWidth, imageHeight)
            zoomEngine.imageSize = imageSize
            val tileDecoder = TileDecoder(
                engine = this@SubsamplingEngine,
                imageSource = imageSource,
                imageSize = imageSize,
                imageMimeType = imageType,
                imageExifOrientation = exifOrientation,
            )
            tileManager = TileManager(
                engine = this@SubsamplingEngine,
                decoder = tileDecoder,
                imageSource = imageSource,
                imageSize = imageSize,
                viewSize = viewSize,
            )
            logger.d(MODULE) { "setImageSource success. viewSize=$viewSize, imageSize=$imageSize, mimeType=$imageType, exifOrientation=${exifOrientation}. '${imageSource.key}'" }
            refreshTiles()
            initJob = null
        }
    }

    @MainThread
    private fun refreshTiles() {
        requiredMainThread()
        val imageSource = imageSource ?: return
        if (destroyed) {
            logger.d(MODULE) { "refreshTiles. interrupted. destroyed. '${imageSource.key}'" }
            return
        }
        if (paused) {
            logger.d(MODULE) { "refreshTiles. interrupted. paused. '${imageSource.key}'" }
            return
        }
        val manager = tileManager
        if (manager == null) {
            logger.d(MODULE) { "refreshTiles. interrupted. initializing. '${imageSource.key}'" }
            return
        }
        if (zoomEngine.rotateDegrees % 90 != 0) {
            logger.d(MODULE) { "refreshTiles. interrupted. rotate degrees must be in multiples of 90. '${imageSource.key}'" }
            return
        }

        val drawableSize = zoomEngine.drawableSize
        val scaling = zoomEngine.isScaling
        val drawMatrix = tempDrawMatrix.apply {
            zoomEngine.getDrawMatrix(this)
        }
        val drawableVisibleRect = tempDrawableVisibleRect.apply {
            zoomEngine.getVisibleRect(this)
        }

        if (drawableVisibleRect.isEmpty) {
            logger.d(MODULE) {
                "refreshTiles. interrupted. drawableVisibleRect is empty. drawableVisibleRect=${drawableVisibleRect}. '${imageSource.key}'"
            }
            tileManager?.clean()
            return
        }

        if (scaling) {
            logger.d(MODULE) {
                "refreshTiles. interrupted. scaling. '${imageSource.key}'"
            }
            return
        }

        if (zoomEngine.scale.format(2) <= zoomEngine.minScale.format(2)) {
            logger.d(MODULE) {
                "refreshTiles. interrupted. minScale. '${imageSource.key}'"
            }
            tileManager?.clean()
            return
        }

        tileManager?.refreshTiles(drawableSize, drawableVisibleRect, drawMatrix)
    }

    @MainThread
    fun onDraw(canvas: Canvas) {
        if (destroyed) return
        val drawableSize = zoomEngine.drawableSize
        val drawMatrix = tempDrawMatrix
        val drawableVisibleRect = tempDrawableVisibleRect
        tileManager?.onDraw(canvas, drawableSize, drawableVisibleRect, drawMatrix)
    }

    @MainThread
    internal fun invalidateView() {
        zoomEngine.view.invalidate()
    }

    @MainThread
    fun destroy() {
        requiredMainThread()
        if (destroyed) return
        logger.d(MODULE) { "destroy. '${imageSource?.key}'" }
        initJob?.cancel("destroy")
        tileManager?.destroy()
        tileManager = null
        imageSource = null
    }

    fun addOnTileChangedListener(listener: OnTileChangedListener) {
        this.onTileChangedListenerList = (onTileChangedListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnTileChangedListener(listener: OnTileChangedListener): Boolean {
        return onTileChangedListenerList?.remove(listener) == true
    }

    fun eachTileList(action: (tile: Tile, load: Boolean) -> Unit) {
        val drawableSize = zoomEngine.drawableSize.takeIf { !it.isEmpty } ?: return
        val drawableVisibleRect = tempDrawableVisibleRect.apply {
            zoomEngine.getVisibleRect(this)
        }.takeIf { !it.isEmpty } ?: return
        tileManager?.eachTileList(drawableSize, drawableVisibleRect, action)
    }
}