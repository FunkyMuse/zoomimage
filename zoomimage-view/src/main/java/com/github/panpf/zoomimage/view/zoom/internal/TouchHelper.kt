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

package com.github.panpf.zoomimage.view.zoom.internal

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.github.panpf.zoomimage.util.OffsetCompat
import com.github.panpf.zoomimage.view.zoom.OnViewLongPressListener
import com.github.panpf.zoomimage.view.zoom.OnViewTapListener
import com.github.panpf.zoomimage.view.zoom.ZoomableEngine
import com.github.panpf.zoomimage.zoom.ContinuousTransformType
import com.github.panpf.zoomimage.zoom.GestureType

internal class TouchHelper(view: View, zoomable: ZoomableEngine) {

    private val gestureDetector: UnifiedGestureDetector
    private var doubleTapPoint: OffsetCompat? = null
    private var doubleTapPressed = false
    private var oneFingerScaleExecuted = false
    private var longPressExecuted = false
    private var panChangeCount = OffsetCompat.Zero
    private var lastPointCount: Int = 0

    var onViewTapListener: OnViewTapListener? = null
    var onViewLongPressListener: OnViewLongPressListener? = null

    init {
        gestureDetector = UnifiedGestureDetector(
            view = view,
            onActionDownCallback = {
                doubleTapPoint = null
                doubleTapPressed = false
                oneFingerScaleExecuted = false
                longPressExecuted = false
                panChangeCount = OffsetCompat.Zero
                lastPointCount = 0
                zoomable.stopAllAnimation("onActionDown")
            },
            onSingleTapConfirmedCallback = { e: MotionEvent ->
                val onViewTapListener = onViewTapListener
                if (onViewTapListener != null) {
                    onViewTapListener.onViewTap(view, e.x, e.y)
                    true
                } else {
                    view.performClick()
                }
            },
            onLongPressCallback = { e: MotionEvent ->
                // Once sliding occurs, the long press can no longer be triggered.
                // todo In the default state of the image, this judgment is invalid when it cannot be moved.
                val touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop
                if (panChangeCount.x < touchSlop && panChangeCount.y < touchSlop) {
                    val onViewLongPressListener = onViewLongPressListener
                    if (onViewLongPressListener != null) {
                        onViewLongPressListener.onViewLongPress(view, e.x, e.y)
                    } else {
                        view.performLongClick()
                    }
                    longPressExecuted = true

                    // Not letting go after double-clicking will trigger a long press, so single-finger zooming should no longer be triggered.
                    doubleTapPoint = null
                    doubleTapPressed = false
                }
            },
            onDoubleTapPressCallback = { e: MotionEvent ->
                doubleTapPoint = OffsetCompat(x = e.x, y = e.y)
                doubleTapPressed = true
                true
            },
            onDoubleTapUpCallback = { e: MotionEvent ->
                doubleTapPoint = null
                doubleTapPressed = false
                if (zoomable.isSupportGestureType(GestureType.DOUBLE_TAP_SCALE) && !oneFingerScaleExecuted && !longPressExecuted) {
                    val touchPoint = OffsetCompat(x = e.x, y = e.y)
                    val centroidContentPoint = zoomable.touchPointToContentPoint(touchPoint)
                    zoomable.switchScale(centroidContentPoint, animated = true)
                }
                true
            },
            canDrag = { horizontal: Boolean, direction: Int ->
                val longPressPoint = doubleTapPoint
                val allowDrag = zoomable.isSupportGestureType(GestureType.DRAG)
                val canScroll = zoomable.canScroll(horizontal, direction)
                val allowOneFingerScale =
                    zoomable.isSupportGestureType(GestureType.ONE_FINGER_SCALE)
                val oneFingerAlready = longPressPoint != null
                (allowDrag && canScroll) || (allowOneFingerScale && oneFingerAlready)
            },
            onGestureCallback = { scaleFactor: Float, focus: OffsetCompat, panChange: OffsetCompat, pointCount: Int ->
                zoomable._continuousTransformTypeState.value = ContinuousTransformType.GESTURE
                panChangeCount += panChange
                lastPointCount = pointCount
                val doubleTapPoint = doubleTapPoint
                val oneFingerScaleSpec = zoomable.oneFingerScaleSpecState.value
                if (pointCount == 1 && doubleTapPoint != null && doubleTapPressed) {
                    if (zoomable.isSupportGestureType(GestureType.ONE_FINGER_SCALE)) {
                        oneFingerScaleExecuted = true
                        val scale = oneFingerScaleSpec.panToScaleTransformer.transform(panChange.y)
                        zoomable.gestureTransform(
                            centroid = doubleTapPoint,
                            panChange = OffsetCompat.Zero,
                            zoomChange = scale,
                            rotationChange = 0f
                        )
                    }
                } else {
                    if (zoomable.isSupportGestureType(GestureType.TWO_FINGER_SCALE)) {
                        val finalPan =
                            if (zoomable.isSupportGestureType(GestureType.DRAG)) panChange else OffsetCompat.Zero
                        zoomable.gestureTransform(
                            centroid = focus,
                            panChange = finalPan,
                            zoomChange = scaleFactor,
                            rotationChange = 0f,
                        )
                    }
                }
            },
            onEndCallback = { focus, velocity ->
                if (zoomable.isSupportGestureType(GestureType.DRAG)) {
                    val pointCount = lastPointCount
                    val doubleTapPoint = doubleTapPoint
                    if (pointCount == 1 && doubleTapPoint != null
                        && zoomable.isSupportGestureType(GestureType.ONE_FINGER_SCALE)
                    ) {
                        zoomable.rollbackScale(doubleTapPoint)
                    } else {
                        if (!zoomable.rollbackScale(focus)) {
                            if (!zoomable.fling(velocity)) {
                                zoomable._continuousTransformTypeState.value =
                                    ContinuousTransformType.NONE
                            }
                        }
                    }
                }
            },
        )
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}