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
package com.github.panpf.zoomimage.view.zoom

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView.ScaleType
import com.github.panpf.zoomimage.Logger
import com.github.panpf.zoomimage.ReadMode
import com.github.panpf.zoomimage.ScrollEdge
import com.github.panpf.zoomimage.util.IntOffsetCompat
import com.github.panpf.zoomimage.util.IntSizeCompat
import com.github.panpf.zoomimage.util.OffsetCompat
import com.github.panpf.zoomimage.util.ScaleFactorCompat
import com.github.panpf.zoomimage.view.internal.isAttachedToWindowCompat
import com.github.panpf.zoomimage.view.zoom.internal.ImageViewBridge
import com.github.panpf.zoomimage.view.zoom.internal.ScrollBarEngine
import com.github.panpf.zoomimage.view.zoom.internal.UnifiedGestureDetector
import com.github.panpf.zoomimage.view.zoom.internal.ZoomEngine

class ZoomAbility constructor(
    private val view: View,
    private val imageViewBridge: ImageViewBridge,
    logger: Logger,
) {
    private val logger = logger.newLogger(module = "ZoomAbility")
    private var scrollBarEngine: ScrollBarEngine? = null
    private val gestureDetector: UnifiedGestureDetector
    private val cacheImageMatrix = Matrix()
    private val cacheVisibleRect = Rect()
    private var onViewTapListenerList: MutableSet<OnViewTapListener>? = null
    private var onViewLongPressListenerList: MutableSet<OnViewLongPressListener>? = null
    internal val zoomEngine = ZoomEngine(logger = this.logger, view = view)

    var scrollBarSpec: ScrollBarSpec? = ScrollBarSpec.Default
        set(value) {
            if (field != value) {
                field = value
                resetScrollBarHelper()
            }
        }
    var threeStepScale: Boolean
        get() = zoomEngine.threeStepScale
        set(value) {
            zoomEngine.threeStepScale = value
        }
    var rubberBandScale: Boolean
        get() = zoomEngine.rubberBandScale
        set(value) {
            zoomEngine.rubberBandScale = value
        }
    var readMode: ReadMode?
        get() = zoomEngine.readMode
        set(value) {
            zoomEngine.readMode = value
        }
    var mediumScaleMinMultiple: Float
        get() = zoomEngine.mediumScaleMinMultiple
        set(value) {
            zoomEngine.mediumScaleMinMultiple = value
        }
    var animationSpec: ZoomAnimationSpec
        get() = zoomEngine.animationSpec
        set(value) {
            zoomEngine.animationSpec = value
        }
    var allowParentInterceptOnEdge: Boolean
        get() = zoomEngine.allowParentInterceptOnEdge
        set(value) {
            zoomEngine.allowParentInterceptOnEdge = value
        }

    init {
        val initScaleType = imageViewBridge.superGetScaleType()
        require(initScaleType != ScaleType.MATRIX) { "ScaleType cannot be MATRIX" }
        imageViewBridge.superSetScaleType(ScaleType.MATRIX)

        zoomEngine.scaleType = initScaleType
        zoomEngine.addOnMatrixChangeListener {
            imageViewBridge.superSetImageMatrix(
                cacheImageMatrix.apply { zoomEngine.getDisplayMatrix(this) }
            )
            scrollBarEngine?.onMatrixChanged()
        }

        gestureDetector = UnifiedGestureDetector(
            context = view.context,
            onDownCallback = { true },
            onSingleTapConfirmedCallback = { e: MotionEvent ->
                val onViewTapListenerList = onViewTapListenerList
                onViewTapListenerList?.forEach {
                    it.onViewTap(view, e.x, e.y)
                }
                onViewTapListenerList?.isNotEmpty() == true || view.performClick()
            },
            onLongPressCallback = { e: MotionEvent ->
                val onViewLongPressListenerList = onViewLongPressListenerList
                onViewLongPressListenerList?.forEach {
                    it.onViewLongPress(view, e.x, e.y)
                }
                onViewLongPressListenerList?.isNotEmpty() == true || view.performLongClick()
            },
            onDoubleTapCallback = { e: MotionEvent ->
                zoomEngine.switchScale(e.x, e.y)
                true
            },
            onDragCallback = { dx: Float, dy: Float, scaling: Boolean ->
                if (!scaling) {
                    zoomEngine.doDrag(dx, dy)
                }
            },
            onFlingCallback = { velocityX: Float, velocityY: Float ->
                zoomEngine.fling(velocityX, velocityY)
            },
            onScaleCallback = { scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float ->
                zoomEngine.doScale(scaleFactor, focusX, focusY, dx, dy)
            },
            onScaleBeginCallback = {
                zoomEngine.manualScaling = true
                true
            },
            onScaleEndCallback = { zoomEngine.manualScaling = false },
            onActionDownCallback = { zoomEngine.actionDown() },
            onActionUpCallback = { zoomEngine.rollbackScale() },
            onActionCancelCallback = { zoomEngine.rollbackScale() },
        )

        resetDrawableSize()
        resetScrollBarHelper()
    }


    /**************************************** Internal ********************************************/

    private fun resetDrawableSize() {
        val drawable = imageViewBridge.getDrawable()
        zoomEngine.drawableSize =
            drawable?.let { IntSizeCompat(it.intrinsicWidth, it.intrinsicHeight) }
                ?: IntSizeCompat.Zero
    }

    private fun resetScrollBarHelper() {
        scrollBarEngine?.cancel()
        scrollBarEngine = null
        val scrollBarSpec = this@ZoomAbility.scrollBarSpec
        if (scrollBarSpec != null) {
            scrollBarEngine = ScrollBarEngine(view, scrollBarSpec)
        }
    }

    private fun destroy() {
        zoomEngine.clean()
    }


    /*************************************** Interaction with consumers ******************************************/

    /**
     * Sets the dimensions of the original image, which is used to calculate the scale of double-click scaling
     */
    fun setImageSize(size: IntSizeCompat?) {
        zoomEngine.imageSize = size ?: IntSizeCompat.Zero
    }

    /**
     * Locate to the location specified on the drawable image. You don't have to worry about scaling and rotation
     */
    fun location(
        offsetOfContent: IntOffsetCompat,
        targetScale: Float = scale.scaleX,
        animated: Boolean = false
    ) {
        zoomEngine.location(offsetOfContent, targetScale, animated)
    }

    /**
     * Scale to the specified scale. You don't have to worry about rotation degrees
     */
    fun scale(
        scale: Float,
        centroid: OffsetCompat = OffsetCompat(viewSize.width / 2f, viewSize.height / 2f),
        animate: Boolean = false
    ) {
        zoomEngine.scale(scale, centroid, animate)
    }

    fun offset(offset: IntOffsetCompat, animate: Boolean = false) {
        zoomEngine.offset(offset, animate)
    }

    /**
     * Rotate the image to the specified degrees
     *
     * @param rotation Rotation degrees, can only be 90°, 180°, 270°, 360°
     */
    fun rotation(rotation: Int) {
        zoomEngine.rotation(rotation)
    }

    fun getNextStepScale(): Float = zoomEngine.getNextStepScale()

    fun canScroll(horizontal: Boolean, direction: Int): Boolean =
        zoomEngine.canScroll(horizontal, direction)

    val rotation: Int
        get() = zoomEngine.rotation

    val scrollEdge: ScrollEdge
        get() = zoomEngine.scrollEdge

    val scaling: Boolean
        get() = zoomEngine.scaling
    val fling: Boolean
        get() = zoomEngine.fling

    val userScale: Float
        get() = zoomEngine.userScale
    val userOffset: OffsetCompat
        get() = zoomEngine.userOffset

    val baseScale: ScaleFactorCompat
        get() = zoomEngine.baseScale
    val baseOffset: OffsetCompat
        get() = zoomEngine.baseOffset

    val scale: ScaleFactorCompat
        get() = zoomEngine.scale
    val offset: OffsetCompat
        get() = zoomEngine.offset

    val minScale: Float
        get() = zoomEngine.minScale
    val mediumScale: Float
        get() = zoomEngine.mediumScale
    val maxScale: Float
        get() = zoomEngine.maxScale

    val viewSize: IntSizeCompat
        get() = zoomEngine.viewSize
    val imageSize: IntSizeCompat
        get() = zoomEngine.imageSize
    val drawableSize: IntSizeCompat
        get() = zoomEngine.drawableSize

    fun getDisplayMatrix(matrix: Matrix) = zoomEngine.getDisplayMatrix(matrix)

    fun getDisplayRect(rectF: RectF) = zoomEngine.getDisplayRect(rectF)

    fun getDisplayRect(): RectF = zoomEngine.getDisplayRect()

    /** Gets the area that the user can see on the drawable (not affected by rotation) */
    fun getVisibleRect(rect: Rect) = zoomEngine.getVisibleRect(rect)

    /** Gets the area that the user can see on the drawable (not affected by rotation) */
    fun getVisibleRect(): Rect = zoomEngine.getVisibleRect()

    fun touchPointToDrawablePoint(touchPoint: PointF): Point? {
        return zoomEngine.touchPointToDrawablePoint(touchPoint)
    }

    fun addOnMatrixChangeListener(listener: OnMatrixChangeListener) {
        zoomEngine.addOnMatrixChangeListener(listener)
    }

    fun removeOnMatrixChangeListener(listener: OnMatrixChangeListener): Boolean {
        return zoomEngine.removeOnMatrixChangeListener(listener)
    }

    fun addOnRotateChangeListener(listener: OnRotateChangeListener) {
        zoomEngine.addOnRotateChangeListener(listener)
    }

    fun removeOnRotateChangeListener(listener: OnRotateChangeListener): Boolean {
        return zoomEngine.removeOnRotateChangeListener(listener)
    }

    fun addOnViewTapListener(listener: OnViewTapListener) {
        this.onViewTapListenerList = (onViewTapListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnViewTapListener(listener: OnViewTapListener): Boolean {
        return onViewTapListenerList?.remove(listener) == true
    }

    fun addOnViewLongPressListener(listener: OnViewLongPressListener) {
        this.onViewLongPressListenerList = (onViewLongPressListenerList ?: LinkedHashSet()).apply {
            add(listener)
        }
    }

    fun removeOnViewLongPressListener(listener: OnViewLongPressListener): Boolean {
        return onViewLongPressListenerList?.remove(listener) == true
    }


    /**************************************** Interact with View ********************************************/

    @Suppress("UNUSED_PARAMETER")
    fun onDrawableChanged(oldDrawable: Drawable?, newDrawable: Drawable?) {
        destroy()
        if (view.isAttachedToWindowCompat) {
            resetDrawableSize()
        }
    }

    fun onAttachedToWindow() {
    }

    fun onDetachedFromWindow() {
        destroy()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        val viewWidth = view.width - view.paddingLeft - view.paddingRight
        val viewHeight = view.height - view.paddingTop - view.paddingBottom
        zoomEngine.viewSize = IntSizeCompat(viewWidth, viewHeight)
    }

    fun onDraw(canvas: Canvas) {
        scrollBarEngine?.onDraw(
            canvas = canvas,
            viewSize = zoomEngine.viewSize,
            contentSize = zoomEngine.drawableSize,
            contentVisibleRect = cacheVisibleRect.apply { zoomEngine.getVisibleRect(this) }
        )
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    fun setScaleType(scaleType: ScaleType): Boolean {
        zoomEngine.scaleType = scaleType
        return true
    }

    fun getScaleType(): ScaleType = zoomEngine.scaleType
}