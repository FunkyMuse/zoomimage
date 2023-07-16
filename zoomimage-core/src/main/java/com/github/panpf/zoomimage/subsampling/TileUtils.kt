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
package com.github.panpf.zoomimage.subsampling

import com.github.panpf.zoomimage.core.IntRectCompat
import com.github.panpf.zoomimage.core.IntSizeCompat
import com.github.panpf.zoomimage.core.internal.format
import com.github.panpf.zoomimage.subsampling.internal.isSupportBitmapRegionDecoder
import kotlin.math.abs
import kotlin.math.ceil

internal fun initializeTileMap(
    imageSize: IntSizeCompat,
    tileMaxSize: IntSizeCompat
): Map<Int, List<Tile>> {
    /* The core rules are: The size of each tile does not exceed tileMaxSize */
    val tileMaxWith = tileMaxSize.width
    val tileMaxHeight = tileMaxSize.height
    val tileMap = HashMap<Int, List<Tile>>()

    var sampleSize = 1
    while (true) {
        var xTiles = 0
        var sourceTileWidth: Int
        var sampleTileWidth: Int
        do {
            xTiles += 1
            sourceTileWidth = ceil(imageSize.width / xTiles.toFloat()).toInt()
            sampleTileWidth = ceil(sourceTileWidth / sampleSize.toFloat()).toInt()
        } while (sampleTileWidth > tileMaxWith)

        var yTiles = 0
        var sourceTileHeight: Int
        var sampleTileHeight: Int
        do {
            yTiles += 1
            sourceTileHeight = ceil(imageSize.height / yTiles.toFloat()).toInt()
            sampleTileHeight = ceil(sourceTileHeight / sampleSize.toFloat()).toInt()
        } while (sampleTileHeight > tileMaxHeight)

        val tileList = ArrayList<Tile>(xTiles * yTiles)
        var left = 0
        var top = 0
        while (true) {
            val right = (left + sourceTileWidth).coerceAtMost(imageSize.width)
            val bottom = (top + sourceTileHeight).coerceAtMost(imageSize.height)
            tileList.add(Tile(IntRectCompat(left, top, right, bottom), sampleSize))
            if (right >= imageSize.width && bottom >= imageSize.height) {
                break
            } else if (right >= imageSize.width) {
                left = 0
                top += sourceTileHeight
            } else {
                left += sourceTileWidth
            }
        }
        tileMap[sampleSize] = tileList

        if (tileList.size == 1) {
            break
        } else {
            sampleSize *= 2
        }
    }
    return tileMap
}

internal fun findSampleSize(
    imageWidth: Int,
    imageHeight: Int,
    drawableWidth: Int,
    drawableHeight: Int,
    scale: Float
): Int {
    require(
        canUseSubsamplingByAspectRatio(
            imageWidth,
            imageHeight,
            drawableWidth,
            drawableHeight
        )
    ) {
        "imageSize(${imageWidth}x${imageHeight}) and drawableSize(${drawableWidth}x${drawableHeight}) must have the same aspect ratio"
    }

    val scaledWidthRatio = (imageWidth / (drawableWidth * scale))
    var sampleSize = 1
    while (scaledWidthRatio >= sampleSize * 2) {
        sampleSize *= 2
    }
    return sampleSize
}

fun canUseSubsamplingByAspectRatio(
    imageWidth: Int, imageHeight: Int, drawableWidth: Int, drawableHeight: Int
): Boolean {
    if (imageWidth == 0 || imageHeight == 0 || drawableWidth == 0 || drawableHeight == 0) return false
    val imageRatio = (imageWidth / imageHeight.toFloat()).format(2)
    val drawableRatio = (drawableWidth / drawableHeight.toFloat()).format(2)
    return abs(imageRatio - drawableRatio).format(2) <= 0.50f
}

fun canUseSubsampling(imageInfo: ImageInfo, drawableSize: IntSizeCompat): Int {
    if (drawableSize.width >= imageInfo.width && drawableSize.height >= imageInfo.height) {
        return -1
    }
    if (!canUseSubsamplingByAspectRatio(
            imageInfo.width, imageInfo.height, drawableSize.width, drawableSize.height
        )
    ) {
        return -2
    }
    if (!isSupportBitmapRegionDecoder(imageInfo.mimeType)) {
        return -3
    }
    return 0
}