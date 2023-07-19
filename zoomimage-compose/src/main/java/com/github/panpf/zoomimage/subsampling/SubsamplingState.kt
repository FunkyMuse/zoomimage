package com.github.panpf.zoomimage.subsampling

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import com.github.panpf.zoomimage.Logger
import com.github.panpf.zoomimage.ZoomableState
import com.github.panpf.zoomimage.compose.Transform
import com.github.panpf.zoomimage.compose.internal.format
import com.github.panpf.zoomimage.compose.internal.isEmpty
import com.github.panpf.zoomimage.compose.internal.isNotEmpty
import com.github.panpf.zoomimage.compose.internal.toCompatIntRect
import com.github.panpf.zoomimage.compose.internal.toCompatIntSize
import com.github.panpf.zoomimage.compose.internal.toShortString
import com.github.panpf.zoomimage.core.IntRectCompat
import com.github.panpf.zoomimage.core.toShortString
import com.github.panpf.zoomimage.subsampling.internal.readImageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun rememberSubsamplingState(
    logger: Logger,
    tileMemoryCache: TileMemoryCache? = null,
    tileBitmapPool: TileBitmapPool? = null,
    ignoreExifOrientation: Boolean = false,
    disallowReuseBitmap: Boolean = false,
    disableMemoryCache: Boolean = false,
    showTileBounds: Boolean = false,
): SubsamplingState {
    val subsamplingState = remember { SubsamplingState(logger) }

    // When ignoreExifOrientation changes, usually contentSize also changes, so no processing is done here
    subsamplingState.ignoreExifOrientation = ignoreExifOrientation
    LaunchedEffect(tileMemoryCache) {  // todo 代价太大了
        subsamplingState.tileMemoryCache = tileMemoryCache
        subsamplingState.resetTileManager("tileMemoryCacheChanged")
    }
    LaunchedEffect(disableMemoryCache) {   // todo 代价太大了
        subsamplingState.disableMemoryCache = disableMemoryCache
        subsamplingState.resetTileManager("disableMemoryCacheChanged")
    }
    LaunchedEffect(tileBitmapPool) {   // todo 代价太大了
        subsamplingState.tileBitmapPool = tileBitmapPool
        subsamplingState.resetTileDecoder("tileBitmapPoolChanged")
    }
    LaunchedEffect(disallowReuseBitmap) {  // todo 代价太大了
        subsamplingState.disallowReuseBitmap = disallowReuseBitmap
        subsamplingState.resetTileDecoder("disallowReuseBitmapChanged")
    }
    subsamplingState.showTileBounds = showTileBounds
    return subsamplingState
}

@Composable
fun BindZoomableStateAndSubsamplingState(
    zoomableState: ZoomableState,
    subsamplingState: SubsamplingState
) {
    val refreshTiles: (caller: String) -> Unit = { caller ->
        if (!zoomableState.scaling && zoomableState.displayTransform.rotation.roundToInt() % 90 == 0) {
            subsamplingState.refreshTiles(
                displayScale = zoomableState.displayTransform.scaleX,
                displayMinScale = zoomableState.minUserScale * zoomableState.baseTransform.scaleX,
                contentVisibleRect = zoomableState.contentVisibleRect,
                caller = caller
            )
        }
    }

    LaunchedEffect(zoomableState.containerSize) {
        // Changes in containerSize cause a large chain reaction that can cause large memory fluctuations.
        // Size animations cause frequent changes in containerSize, so a delayed reset avoids this problem
        if (subsamplingState.containerSize.isNotEmpty()) {
            delay(60)
        }
        subsamplingState.containerSize = zoomableState.containerSize
        subsamplingState.resetTileManager("containerSizeChanged")
    }
    LaunchedEffect(zoomableState.contentSize) {
        subsamplingState.contentSize = zoomableState.contentSize
        subsamplingState.resetTileDecoder("contentSizeChanged")
    }
    LaunchedEffect(subsamplingState.ready) {
        val imageInfo = subsamplingState.imageInfo
        zoomableState.contentOriginSize = if (subsamplingState.ready && imageInfo != null) {
            IntSize(imageInfo.width, imageInfo.height)
        } else {
            IntSize.Zero
        }
        refreshTiles("imageInfoChanged")
    }
    LaunchedEffect(zoomableState.displayTransform) {
        refreshTiles("displayTransformChanged")
    }
    LaunchedEffect(zoomableState.scaling) {
        refreshTiles("scalingChanged")
    }
}

class SubsamplingState(logger: Logger) : RememberObserver {

    private var logger: Logger = logger.newLogger(module = "SubsamplingState")
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastResetTileDecoderJob: Job? = null
    private var imageSource: ImageSource? = null
    private var tileManager: TileManager? = null
    private var tileDecoder: TileDecoder? = null
    private var lastDisplayScale: Float? = null
    private var lastDisplayMinScale: Float? = null
    private var lastContentVisibleRect: IntRect? = null

    var containerSize: IntSize by mutableStateOf(IntSize.Zero)
    var contentSize: IntSize by mutableStateOf(IntSize.Zero)
    var imageInfo: ImageInfo? by mutableStateOf(null)
    var ignoreExifOrientation: Boolean = false
    var disallowReuseBitmap: Boolean = false
    var disableMemoryCache: Boolean = false
    var showTileBounds: Boolean by mutableStateOf(false)
    var tileBitmapPool: TileBitmapPool? = null
    var tileMemoryCache: TileMemoryCache? = null
    var paused = false
        set(value) {
            if (field != value) {
                field = value
                if (ready) {
                    if (value) {
                        imageSource?.run { logger.d { "pause. '$key'" } }
                        tileManager?.clean("paused")
                    } else {
                        imageSource?.run { logger.d { "resume. '$key'" } }
                        refreshTiles("resume")
                    }
                }
            }
        }

    var ready by mutableStateOf(false)
        private set
    var tilesChanged by mutableStateOf(0)
        private set
    val tileList: List<Tile>
        get() = tileManager?.tileList ?: emptyList()
    val imageLoadRect: IntRectCompat
        get() = tileManager?.imageLoadRect ?: IntRectCompat.Zero
    val imageVisibleRect: IntRectCompat
        get() = tileManager?.imageVisibleRect ?: IntRectCompat.Zero

    fun setImageSource(imageSource: ImageSource?) {
        if (this.imageSource == imageSource) return
        cleanTileManager("setImageSource")
        cleanTileDecoder("setImageSource")
        this.imageSource = imageSource
        resetTileDecoder("setImageSource")
    }

    private fun notifyTileChange() {
        if (tilesChanged < Int.MAX_VALUE) {
            tilesChanged++
        } else {
            tilesChanged = 0
        }
    }

    private fun notifyReadyChange() {
        ready = imageInfo != null && tileDecoder != null && tileManager != null
    }

    private fun cleanTileDecoder(caller: String) {
        val lastResetTileDecoderJob = this@SubsamplingState.lastResetTileDecoderJob
        if (lastResetTileDecoderJob != null) {
            lastResetTileDecoderJob.cancel("$caller:cleanTileDecoder")
            this@SubsamplingState.lastResetTileDecoderJob = null
        }
        val tileDecoder = this@SubsamplingState.tileDecoder
        if (tileDecoder != null) {
            tileDecoder.destroy("$caller:cleanTileDecoder")
            this@SubsamplingState.tileDecoder = null
            logger.d { "cleanTileDecoder. $caller. '${imageSource?.key}'" }
            notifyReadyChange()
        }
        imageInfo = null
    }

    private fun cleanTileManager(caller: String) {
        val tileManager = this@SubsamplingState.tileManager
        if (tileManager != null) {
            tileManager.clean("$caller:cleanTileManager")
            this@SubsamplingState.tileManager = null
            logger.d { "cleanTileManager. $caller. '${imageSource?.key}'" }
            notifyReadyChange()
            notifyTileChange()
        }
    }

    fun resetTileDecoder(caller: String) {
        cleanTileManager("$caller:resetTileDecoder")
        cleanTileDecoder("$caller:resetTileDecoder")

        val imageSource = imageSource ?: return
        val contentSize = contentSize.takeIf { !it.isEmpty() } ?: return

        lastResetTileDecoderJob = coroutineScope.launch(Dispatchers.Main) {
            val imageInfo = imageSource.readImageInfo(ignoreExifOrientation)
            this@SubsamplingState.imageInfo = imageInfo
            val result =
                imageInfo?.let { canUseSubsampling(it, contentSize.toCompatIntSize()) } ?: -10
            if (imageInfo != null && result >= 0) {
                logger.d {
                    "resetTileDecoder success. $caller. " +
                            "contentSize=${contentSize.toShortString()}, " +
                            "ignoreExifOrientation=${ignoreExifOrientation}. " +
                            "imageInfo=${imageInfo.toShortString()}. " +
                            "'${imageSource.key}'"
                }
                this@SubsamplingState.tileDecoder = TileDecoder(
                    logger = logger,
                    imageSource = imageSource,
                    tileBitmapPool = if (disallowReuseBitmap) null else tileBitmapPool,
                    imageInfo = imageInfo,
                )
                resetTileManager(caller)
            } else {
                val cause = when (result) {
                    -1 -> "The content size is greater than or equal to the original image"
                    -2 -> "The content aspect ratio is different with the original image"
                    -3 -> "Image type not support subsampling"
                    -10 -> "Can't decode image bounds or exif orientation"
                    else -> "Unknown"
                }
                logger.d {
                    "resetTileDecoder failed. $caller. $cause. " +
                            "contentSize: ${contentSize.toShortString()}, " +
                            "imageInfo: ${imageInfo?.toShortString()}. " +
                            "'${imageSource.key}'"
                }
            }
            lastResetTileDecoderJob = null
        }
    }

    fun resetTileManager(caller: String) {
        cleanTileManager(caller)

        val imageSource = imageSource ?: return
        val tileDecoder = tileDecoder ?: return
        val imageInfo = imageInfo ?: return
        val containerSize = containerSize.takeIf { !it.isEmpty() } ?: return

        val tileManager = TileManager(
            logger = logger,
            tileDecoder = tileDecoder,
            imageSource = imageSource,
            containerSize = containerSize.toCompatIntSize(),
            tileBitmapPool = if (disallowReuseBitmap) null else tileBitmapPool,
            tileMemoryCache = if (disableMemoryCache) null else tileMemoryCache,
            imageInfo = imageInfo,
            onTileChanged = { notifyTileChange() }
        )
        logger.d {
            val tileMaxSize = tileManager.tileMaxSize
            val tileMap = tileManager.tileMap
            val tileMapInfoList = tileMap.keys.sortedDescending()
                .map { "${it}:${tileMap[it]?.size}" }
            "resetTileManager success. $caller. " +
                    "containerSize=${containerSize.toShortString()}, " +
                    "imageInfo=${imageInfo.toShortString()}. " +
                    "tileMaxSize=${tileMaxSize.toShortString()}, " +
                    "tileMap=$tileMapInfoList, " +
                    "'${imageSource.key}'"
        }
        this@SubsamplingState.tileManager = tileManager
        notifyReadyChange()
        notifyTileChange()
    }

    fun refreshTiles(
        displayScale: Float,
        displayMinScale: Float,
        contentVisibleRect: IntRect,
        caller: String,
    ) {
        this.lastDisplayScale = displayScale
        this.lastDisplayMinScale = displayMinScale
        this.lastContentVisibleRect = contentVisibleRect
        val imageSource = imageSource ?: return
        val tileManager = tileManager ?: return
        val contentSize = contentSize.takeIf { it.isNotEmpty() } ?: return
        if (paused) {
            logger.d { "refreshTiles. $caller. interrupted. paused. '${imageSource.key}'" }
            return
        }
        if (contentVisibleRect.isEmpty) {
            logger.d {
                "refreshTiles. $caller. interrupted. contentVisibleRect is empty. " +
                        "contentVisibleRect=${contentVisibleRect}. '${imageSource.key}'"
            }
            tileManager.clean("refreshTiles:contentVisibleRectEmpty")
            return
        }
        if (displayScale.format(2) <= displayMinScale.format(2)) {
            logger.d { "refreshTiles. $caller. interrupted. Reach minScale. '${imageSource.key}'" }
            tileManager.clean("refreshTiles:reachMinScale")
            return
        }
        tileManager.refreshTiles(
            contentSize = contentSize.toCompatIntSize(),
            contentVisibleRect = contentVisibleRect.toCompatIntRect(),
            scale = displayScale,
            caller = caller
        )
    }

    fun drawTiles(drawScope: DrawScope, baseTransform: Transform) {
        val imageSource = imageSource ?: return
        val imageInfo = imageInfo ?: return
        val tileList = tileList.takeIf { it.isNotEmpty() } ?: return
        val contentSize = contentSize.takeIf { !it.isEmpty() } ?: return
        val imageLoadRect = imageLoadRect.takeIf { !it.isEmpty } ?: return
        val widthScale = imageInfo.width / (contentSize.width * baseTransform.scaleX)
        val heightScale = imageInfo.height / (contentSize.height * baseTransform.scaleY)
        var insideLoadCount = 0
        var outsideLoadCount = 0
        var realDrawCount = 0
        tileList.forEach { tile ->
            if (tile.srcRect.overlaps(imageLoadRect)) {
                insideLoadCount++
                val tileBitmap = tile.bitmap
                val tileSrcRect = tile.srcRect
                val tileDrawRect = IntRect(
                    left = floor(tileSrcRect.left / widthScale).toInt(),
                    top = floor(tileSrcRect.top / heightScale).toInt(),
                    right = floor(tileSrcRect.right / widthScale).toInt(),
                    bottom = floor(tileSrcRect.bottom / heightScale).toInt()
                ).translate(baseTransform.offset.round())
                if (tileBitmap != null) {
                    realDrawCount++
                    val srcRect = IntRect(0, 0, tileBitmap.width, tileBitmap.height)
                    drawScope.drawImage(
                        image = tileBitmap.asImageBitmap(),
                        srcOffset = srcRect.topLeft,
                        srcSize = srcRect.size,
                        dstOffset = tileDrawRect.topLeft,
                        dstSize = tileDrawRect.size,
//                        alpha = 0.5f,
                    )
                }

                if (showTileBounds) {
                    val boundsColor = when {
                        tileBitmap != null -> Color.Green
                        tile.loadJob?.isActive == true -> Color.Yellow
                        else -> Color.Red
                    }
                    val boundsStrokeWidth = 1f * Resources.getSystem().displayMetrics.density
                    val boundsStrokeHalfWidth = boundsStrokeWidth / 2
                    val tileBoundsRect = Rect(
                        left = floor(tileDrawRect.left + boundsStrokeHalfWidth),
                        top = floor(tileDrawRect.top + boundsStrokeHalfWidth),
                        right = ceil(tileDrawRect.right - boundsStrokeHalfWidth),
                        bottom = ceil(tileDrawRect.bottom - boundsStrokeHalfWidth)
                    )
                    drawScope.drawRect(
                        color = boundsColor,
                        topLeft = tileBoundsRect.topLeft,
                        size = tileBoundsRect.size,
                        style = Stroke(width = boundsStrokeWidth),
                        alpha = 0.5f,
                    )
                }
            } else {
                outsideLoadCount++
            }
        }
        logger.d {
            "drawTiles. tiles=${tileList.size}, " +
                    "insideLoadCount=${insideLoadCount}, " +
                    "outsideLoadCount=${outsideLoadCount}, " +
                    "realDrawCount=${realDrawCount}. " +
                    "'${imageSource.key}'"
        }
    }

    override fun onRemembered() {
    }

    override fun onForgotten() {
        destroy("onForgotten")
    }

    override fun onAbandoned() {
        destroy("onAbandoned")
    }

    private fun destroy(caller: String) {
        cleanTileManager("destroy:$caller")
        cleanTileDecoder("destroy:$caller")
    }

    private fun refreshTiles(@Suppress("SameParameterValue") caller: String) {
        val displayScale = lastDisplayScale
        val lastDisplayMinScale = lastDisplayMinScale
        val drawableVisibleRect = lastContentVisibleRect
        if (displayScale != null && lastDisplayMinScale != null && drawableVisibleRect != null) {
            refreshTiles(
                displayScale = displayScale,
                displayMinScale = lastDisplayMinScale,
                contentVisibleRect = drawableVisibleRect,
                caller = caller
            )
        }
    }
}