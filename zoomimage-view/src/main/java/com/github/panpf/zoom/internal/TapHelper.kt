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
package com.github.panpf.zoom.internal

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent

internal class TapHelper constructor(
    context: Context, private val zoomerHelper: ZoomerHelper
) : SimpleOnGestureListener() {

    private val view = zoomerHelper.view
    private val tapGestureDetector: GestureDetector = GestureDetector(context, this)

    fun onTouchEvent(event: MotionEvent): Boolean = tapGestureDetector.onTouchEvent(event)

    override fun onDown(e: MotionEvent): Boolean = true

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean =
        zoomerHelper.onViewTapListener?.onViewTap(view, e.x, e.y) != null || view.performClick()

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        zoomerHelper.onViewLongPressListener?.onViewLongPress(view, e.x, e.y)
            ?: view.performLongClick()
    }

    override fun onDoubleTap(ev: MotionEvent): Boolean {
        try {
            val currentScaleFormat = zoomerHelper.scale.format(2)
            var finalScale = -1f
            for (scale in zoomerHelper.scaleState.doubleClickSteps) {
                if (finalScale == -1f) {
                    finalScale = scale
                } else if (currentScaleFormat < scale.format(2)) {
                    finalScale = scale
                    break
                }
            }
            if (finalScale > currentScaleFormat) {
                zoomerHelper.scale(finalScale, ev.x, ev.y, true)
            } else {
                zoomerHelper.scale(finalScale, true)
            }
        } catch (e: ArrayIndexOutOfBoundsException) {
            // Can sometimes happen when getX() and getY() is called
        }
        return true
    }
}