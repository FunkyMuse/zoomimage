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
package com.github.panpf.zoom.sample.ui.widget

import android.content.Context
import android.util.AttributeSet
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.ImageOptionsProvider
import com.github.panpf.zoom.ZoomImageView

open class SketchZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ZoomImageView(context, attrs, defStyle), ImageOptionsProvider {

    override var displayImageOptions: ImageOptions? = null

    // 不采样时，不需要图片的原始尺寸
//    override fun onDrawableChanged(oldDrawable: Drawable?, newDrawable: Drawable?) {
//        super.onDrawableChanged(oldDrawable, newDrawable)
//        val sketchDrawable = newDrawable?.findLastSketchDrawable()
//        val imageSize = sketchDrawable
//            ?.let { Size(it.imageInfo.width, it.imageInfo.height) }
//            ?: Size.EMPTY
//        _zoomAbility?.setImageSize(imageSize)
//    }
}