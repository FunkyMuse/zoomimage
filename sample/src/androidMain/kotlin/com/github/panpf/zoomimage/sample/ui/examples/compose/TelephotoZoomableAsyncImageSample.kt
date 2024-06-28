package com.github.panpf.zoomimage.sample.ui.examples.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.panpf.sketch.getBitmapOrNull
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.ImageResult
import com.github.panpf.sketch.request.execute
import com.github.panpf.sketch.sketch
import com.github.panpf.zoomimage.sample.settingsService
import com.github.panpf.zoomimage.sample.ui.util.toShortString
import com.github.panpf.zoomimage.sample.ui.util.valueOf
import com.github.panpf.zoomimage.sketch.SketchImageSource
import kotlinx.coroutines.runBlocking
import me.saket.telephoto.subsamplingimage.SubSamplingImage
import me.saket.telephoto.subsamplingimage.SubSamplingImageSource
import me.saket.telephoto.subsamplingimage.rememberSubSamplingImageState
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState

// TODO delete
@Composable
fun TelephotoZoomableAsyncImageSample(sketchImageUri: String) {
    val context = LocalContext.current
    val settingsService = remember { context.settingsService }
    val contentScaleName by settingsService.contentScale.collectAsState()
    val alignmentName by settingsService.alignment.collectAsState()
    val contentScale = remember(contentScaleName) { ContentScale.valueOf(contentScaleName) }
    val alignment = remember(alignmentName) { Alignment.valueOf(alignmentName) }
    val zoomableState = rememberZoomableState(
        zoomSpec = ZoomSpec(maxZoomFactor = 8f)
    )
    LaunchedEffect(Unit) {
        snapshotFlow { contentScale }.collect {
            zoomableState.contentScale = it
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { alignment }.collect {
            zoomableState.contentAlignment = it
        }
    }
    val info = remember(zoomableState.contentTransformation) {
        zoomableState.contentTransformation.run {
            """
                scale: ${scale.toShortString()}
                offset: ${offset.toShortString()}
            """.trimIndent()
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        var loading by remember { mutableStateOf(false) }
        var subSamplingImageSource by remember { mutableStateOf<SubSamplingImageSource?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(sketchImageUri) {
            loading = true
            val imageResult = ImageRequest(context, sketchImageUri).execute()
            if (imageResult is ImageResult.Success) {
                val preview = imageResult.image.getBitmapOrNull()?.asImageBitmap()
                val imageSource = SketchImageSource(context, context.sketch, sketchImageUri)
                subSamplingImageSource = SubSamplingImageSource.rawSource(
                    source = { runBlocking { imageSource.openSource() }.getOrThrow() },
                    preview = preview
                )
            } else {
                errorMessage = (imageResult as ImageResult.Error).throwable.message
            }
            loading = false
        }
        val subSamplingImageSource1 = subSamplingImageSource
        if (loading) {
            CircularProgressIndicator(Modifier.size(50.dp).align(Alignment.Center))
        } else if (subSamplingImageSource1 != null) {
            SubSamplingImage(
                state = rememberSubSamplingImageState(subSamplingImageSource1, zoomableState),
                contentDescription = "",
                modifier = Modifier
                    .fillMaxSize(),
//            onClick = {
//                context.showShortToast("Click (${it.toShortString()})")
//            },
//            onLongClick = {
//                context.showShortToast("Long click (${it.toShortString()})")
//            }
            )
        } else {
            Text(
                text = errorMessage ?: "Load image failed",
                color = Color.White,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp).align(Alignment.Center)
            )
        }

        Text(
            text = info,
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            style = LocalTextStyle.current.copy(
                shadow = Shadow(offset = Offset(1f, 1f), blurRadius = 4f),
            ),
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(10.dp)
        )
    }
}