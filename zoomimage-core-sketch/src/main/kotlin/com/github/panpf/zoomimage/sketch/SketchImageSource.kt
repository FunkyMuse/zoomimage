/*
 * Copyright (C) 2023 panpf <panpfpanpf@outlook.com>
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

package com.github.panpf.zoomimage.sketch

import android.content.Context
import androidx.annotation.WorkerThread
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.datasource.BasedStreamDataSource
import com.github.panpf.sketch.request.Depth
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.zoomimage.subsampling.ImageSource
import kotlinx.coroutines.runBlocking
import java.io.InputStream

class SketchImageSource(
    private val context: Context,
    private val sketch: Sketch,
    private val imageUri: String,
) : ImageSource {

    override val key: String = imageUri

    @WorkerThread
    override fun openInputStream(): Result<InputStream> = kotlin.runCatching {
        val request = LoadRequest(context, imageUri) {
            downloadCachePolicy(CachePolicy.ENABLED)
            depth(Depth.LOCAL)   // Do not download image, by default go here The image have been downloaded
        }
        val fetcher = try {
            sketch.components.newFetcherOrThrow(request)
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val fetchResult = runBlocking {
            fetcher.fetch()
        }.let {
            it.getOrNull() ?: return Result.failure(it.exceptionOrNull()!!)
        }
        val dataSource = fetchResult.dataSource
        if (dataSource !is BasedStreamDataSource) {
            return Result.failure(IllegalStateException("DataSource is not BasedStreamDataSource. imageUri='$imageUri'"))
        }
        dataSource.newInputStream()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SketchImageSource
        if (context != other.context) return false
        if (sketch != other.sketch) return false
        if (imageUri != other.imageUri) return false
        return true
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + sketch.hashCode()
        result = 31 * result + imageUri.hashCode()
        return result
    }

    override fun toString(): String {
        return "SketchImageSource('$imageUri')"
    }
}