package com.github.panpf.zoomimage.core.glide.test

import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.github.panpf.zoomimage.glide.GlideHttpImageSource
import com.github.panpf.zoomimage.glide.GlideModelToImageSourceImpl
import com.github.panpf.zoomimage.subsampling.AssetImageSource
import com.github.panpf.zoomimage.subsampling.ByteArrayImageSource
import com.github.panpf.zoomimage.subsampling.ContentImageSource
import com.github.panpf.zoomimage.subsampling.FileImageSource
import com.github.panpf.zoomimage.subsampling.ImageSource.WrapperFactory
import com.github.panpf.zoomimage.subsampling.ResourceImageSource
import com.github.panpf.zoomimage.subsampling.toFactory
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.io.File
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals

class GlideModelToImageSourceImplTest {

    @Test
    fun test() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().context
        val glide = Glide.get(context)
        val modelToImageSource = GlideModelToImageSourceImpl()

        val httpUri = "http://www.example.com/image.jpg"
        assertEquals(
            expected = GlideHttpImageSource.Factory(glide, httpUri),
            actual = modelToImageSource.modelToImageSource(context, Glide.get(context), httpUri)
        )
        assertEquals(
            expected = GlideHttpImageSource.Factory(glide, httpUri),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                httpUri.toUri()
            )
        )
        assertEquals(
            expected = GlideHttpImageSource.Factory(glide, httpUri),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                URL(httpUri)
            )
        )
        assertEquals(
            expected = GlideHttpImageSource.Factory(glide, httpUri),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                GlideUrl(httpUri)
            )
        )

        val httpsUri = "https://www.example.com/image.jpg"
        assertEquals(
            expected = GlideHttpImageSource.Factory(glide, httpsUri),
            actual = modelToImageSource.modelToImageSource(context, Glide.get(context), httpsUri)
        )
        assertEquals(
            expected = GlideHttpImageSource.Factory(glide, httpsUri),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                httpsUri.toUri()
            )
        )
        assertEquals(
            expected = GlideHttpImageSource.Factory(glide, httpsUri),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                URL(httpsUri)
            )
        )
        assertEquals(
            expected = GlideHttpImageSource.Factory(glide, httpsUri),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                GlideUrl(httpsUri)
            )
        )

        val contentUri = "content://myapp/image.jpg"
        assertEquals(
            expected = ContentImageSource(context, contentUri.toUri()).toFactory(),
            actual = modelToImageSource.modelToImageSource(context, Glide.get(context), contentUri)
        )
        assertEquals(
            expected = ContentImageSource(context, contentUri.toUri()).toFactory(),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                contentUri.toUri()
            )
        )

        val assetUri = "file:///android_asset/image.jpg"
        val assetFileName = assetUri.toUri().pathSegments.drop(1).joinToString("/")
        assertEquals(
            expected = AssetImageSource(context, assetFileName).toFactory(),
            actual = modelToImageSource.modelToImageSource(context, Glide.get(context), assetUri)
        )
        assertEquals(
            expected = AssetImageSource(context, assetFileName).toFactory(),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                assetUri.toUri()
            )
        )

        val pathUri = "/sdcard/image.jpg"
        assertEquals(
            expected = FileImageSource(pathUri.toPath()).toFactory(),
            actual = modelToImageSource.modelToImageSource(context, Glide.get(context), pathUri)
        )
        assertEquals(
            expected = FileImageSource(pathUri.toPath()).toFactory(),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                pathUri.toUri()
            )
        )

        val fileUri = "file:///sdcard/image.jpg"
        assertEquals(
            expected = FileImageSource(fileUri.toUri().path!!.toPath()).toFactory(),
            actual = modelToImageSource.modelToImageSource(context, Glide.get(context), fileUri)
        )
        assertEquals(
            expected = FileImageSource(fileUri.toUri().path!!.toPath()).toFactory(),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                fileUri.toUri()
            )
        )
        assertEquals(
            expected = FileImageSource(fileUri.toUri().path!!.toPath()).toFactory(),
            actual = modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                fileUri.toUri()
            )
        )

        val file = File("/sdcard/image.jpg")
        assertEquals(
            expected = FileImageSource(file).toFactory(),
            actual = modelToImageSource.modelToImageSource(context, Glide.get(context), file)
        )

        val resourceId = com.github.panpf.zoomimage.images.R.raw.huge_card
        assertEquals(
            expected = ResourceImageSource(context, resourceId).toFactory(),
            actual = modelToImageSource.modelToImageSource(context, Glide.get(context), resourceId)
        )

        val resourceNameUri = "android.resource://${context.packageName}/raw/huge_card"
        assertEquals(
            expected = resourceId,
            actual = ((modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                resourceNameUri
            ) as WrapperFactory).imageSource as ResourceImageSource).resId
        )
        assertEquals(
            expected = resourceId,
            actual = ((modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                resourceNameUri.toUri()
            ) as WrapperFactory).imageSource as ResourceImageSource).resId
        )
        assertEquals(
            expected = resourceId,
            actual = ((modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                resourceNameUri.toUri()
            ) as WrapperFactory).imageSource as ResourceImageSource).resId
        )

        val resourceIntUri = "android.resource://${context.packageName}/${resourceId}"
        assertEquals(
            expected = resourceId,
            actual = ((modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                resourceIntUri
            ) as WrapperFactory).imageSource as ResourceImageSource).resId
        )
        assertEquals(
            expected = resourceId,
            actual = ((modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                resourceIntUri.toUri()
            ) as WrapperFactory).imageSource as ResourceImageSource).resId
        )
        assertEquals(
            expected = resourceId,
            actual = ((modelToImageSource.modelToImageSource(
                context,
                Glide.get(context),
                resourceIntUri.toUri()
            ) as WrapperFactory).imageSource as ResourceImageSource).resId
        )

        val byteArray = "Hello".toByteArray()
        assertEquals(
            expected = ByteArrayImageSource(byteArray).toFactory(),
            actual = modelToImageSource.modelToImageSource(context, Glide.get(context), byteArray)
        )
    }
}