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

package com.github.panpf.zoomimage.util

import com.github.panpf.zoomimage.subsampling.SkiaRect

/**
 * Convert [IntRectCompat] to [SkiaRect]
 *
 * @see com.github.panpf.zoomimage.core.nonandroid.test.util.UtilsNonAndroidTest.testToSkiaRect
 */
internal fun IntRectCompat.toSkiaRect(): SkiaRect = SkiaRect(
    left = left.toFloat(),
    top = top.toFloat(),
    right = right.toFloat(),
    bottom = bottom.toFloat(),
)