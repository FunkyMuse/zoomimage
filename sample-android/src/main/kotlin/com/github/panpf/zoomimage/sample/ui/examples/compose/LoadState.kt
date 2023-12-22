package com.github.panpf.zoomimage.sample.ui.examples.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.compose.AsyncImageState
import com.github.panpf.sketch.compose.rememberAsyncImageState
import com.github.panpf.sketch.request.LoadState
import com.github.panpf.zoomimage.sample.R

@Composable
fun LoadState(imageState: AsyncImageState, modifier: Modifier = Modifier) {
    if (imageState.loadState is LoadState.Error || LocalInspectionMode.current) {
        Column(
            modifier = modifier
                .size(200.dp)
                .background(Color(0xEE2E2E2E), RoundedCornerShape(16.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_error),
                contentDescription = "icon",
                tint = Color.White
            )

            Spacer(modifier = Modifier.size(6.dp))
            Text(text = "Display failure", color = Color.White)

            Spacer(modifier = Modifier.size(24.dp))
            Button(
                onClick = {
                    imageState.restart()
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(text = "Retry")
            }
        }
    }
}

@Preview
@Composable
fun LoadStatePreview() {
    LoadState(rememberAsyncImageState())
}