package com.github.panpf.zoomimage.sample.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.panpf.sketch.request.ImageResult
import com.github.panpf.sketch.size
import com.github.panpf.zoomimage.sample.ui.getSettingsDialogHeight

@Composable
fun PhotoInfoDialog(imageResult: ImageResult?, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties()) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = getSettingsDialogHeight())
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val uri: String? = imageResult?.request?.uri
            PhotoInfoItem(null, uri.orEmpty())

            if (imageResult is ImageResult.Success) {
                val optionsInfo = imageResult.cacheKey
                    .replace(imageResult.request.uri, "")
                    .let { if (it.startsWith("?")) it.substring(1) else it }
                    .split("&")
                    .filter { it.trim().isNotEmpty() }
                    .joinToString(separator = "\n")
                PhotoInfoItem("Options: ", optionsInfo)

                val sourceImageInfo = remember {
                    imageResult.imageInfo.run {
                        "${width}x${height}, ${mimeType}"
                    }
                }
                PhotoInfoItem("Source Image: ", sourceImageInfo)

                PhotoInfoItem("Result Image: ", "${imageResult.image.size}")

                val dataFromInfo = imageResult.dataFrom.name
                PhotoInfoItem("Data From: ", dataFromInfo)

                val transformedInfo = imageResult.transformeds
                    ?.joinToString(separator = "\n") { transformed ->
                        transformed.replace("Transformed", "")
                    }
                PhotoInfoItem("Transformeds: ", transformedInfo.orEmpty())
            } else if (imageResult is ImageResult.Error) {
                val throwableString = imageResult.throwable.toString()
                PhotoInfoItem("Throwable: ", throwableString)
            }
        }
    }
}

@Composable
fun PhotoInfoItem(title: String? = null, content: String) {
    Column {
        if (title != null) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Text(
            text = content,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}