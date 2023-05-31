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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.updateLayoutParams
import com.github.panpf.tools4a.dimen.ktx.dp2pxF
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class TilesMapImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    private val tileBoundsPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f.dp2pxF
    }
    private val strokeHalfWidth = tileBoundsPaint.strokeWidth / 2
    private val drawableVisibleRect = Rect()
    private val mapVisibleRect = Rect()
    private val tileDrawRect = Rect()
    private var zoomView: MyZoomImageView? = null

    private val detector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            location(e.x, e.y)
            return true
        }
    })

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth = width.takeIf { it > 0 } ?: return
        val zoomView = zoomView ?: return
        val imageSize = zoomView.imageSize?.takeIf { !it.isEmpty } ?: return
        val drawableSize = zoomView.drawableSize?.takeIf { !it.isEmpty } ?: return
        val drawableVisibleRect = drawableVisibleRect
            .apply { zoomView.getVisibleRect(this) }
            .takeIf { !it.isEmpty } ?: return
        val targetScale = imageSize.width.toFloat() / viewWidth

        zoomView.eachTileList { tile, load ->
            val tileBitmap = tile.bitmap
            val tileSrcRect = tile.srcRect
            val tileDrawRect = tileDrawRect.apply {
                set(
                    floor((tileSrcRect.left / targetScale) + strokeHalfWidth).toInt(),
                    floor((tileSrcRect.top / targetScale) + strokeHalfWidth).toInt(),
                    ceil((tileSrcRect.right / targetScale) - strokeHalfWidth).toInt(),
                    ceil((tileSrcRect.bottom / targetScale) - strokeHalfWidth).toInt()
                )
            }
            val boundsColor = when {
                !load -> Color.parseColor("#00BFFF")
                tileBitmap != null -> Color.GREEN
                tile.loadJob?.isActive == true -> Color.YELLOW
                else -> Color.RED
            }
            tileBoundsPaint.color = boundsColor
            canvas.drawRect(tileDrawRect, tileBoundsPaint)
        }

        val mapVisibleRect = mapVisibleRect.apply {
            val mapScaled = drawableSize.width / viewWidth.toFloat()
            set(
                floor(drawableVisibleRect.left / mapScaled).toInt(),
                floor(drawableVisibleRect.top / mapScaled).toInt(),
                ceil(drawableVisibleRect.right / mapScaled).toInt(),
                ceil(drawableVisibleRect.bottom / mapScaled).toInt()
            )
        }
        tileBoundsPaint.color = Color.parseColor("#FF00FF")
        canvas.drawRect(mapVisibleRect, tileBoundsPaint)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        resetViewSize()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        detector.onTouchEvent(event)
        return true
    }

    fun setZoomImageView(zoomView: MyZoomImageView) {
        this.zoomView = zoomView
        zoomView.addOnMatrixChangeListener {
            invalidate()
        }
        zoomView.addOnTileChangedListener {
            invalidate()
        }
    }

    private fun resetViewSize(): Boolean {
        val drawable = drawable ?: return true
        val zoomView = zoomView ?: return true

        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        val zoomViewWidth = zoomView.width
        val zoomViewHeight = zoomView.height
        if ((zoomViewWidth / drawableWidth.toFloat()) < (zoomViewHeight / drawableHeight.toFloat())) {
            val ratio = when {
                drawableWidth / drawableHeight > 4 -> 0.7f
                zoomViewWidth >= zoomViewHeight -> 0.55f
                else -> 0.4f
            }
            val viewWidth = (zoomViewWidth * ratio).roundToInt()
            val viewHeight = (drawableHeight * (viewWidth / drawableWidth.toFloat())).roundToInt()
            updateLayoutParams<LayoutParams> {
                width = viewWidth
                height = viewHeight
            }
        } else {
            val ratio = when {
                drawableHeight / drawableWidth > 4 -> 0.7f
                zoomViewWidth < zoomViewHeight -> 0.55f
                else -> 0.4f
            }
            val viewHeight = (zoomViewHeight * ratio).roundToInt()
            val viewWidth = (drawableWidth * (viewHeight / drawableHeight.toFloat())).roundToInt()
            updateLayoutParams<LayoutParams> {
                width = viewWidth
                height = viewHeight
            }
        }
        return true
    }

    private fun location(x: Float, y: Float) {
        val zoomView = zoomView ?: return
        val viewWidth = width.takeIf { it > 0 } ?: return
        val viewHeight = height.takeIf { it > 0 } ?: return
        val drawable = zoomView.drawable
            ?.takeIf { it.intrinsicWidth != 0 && it.intrinsicHeight != 0 }
            ?: return

        val widthScale = drawable.intrinsicWidth.toFloat() / viewWidth
        val heightScale = drawable.intrinsicHeight.toFloat() / viewHeight
        val realX = x * widthScale
        val realY = y * heightScale

        zoomView.location(realX, realY, animate = true)
    }
}
