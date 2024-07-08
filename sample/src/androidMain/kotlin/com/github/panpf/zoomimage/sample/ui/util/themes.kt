package com.github.panpf.zoomimage.sample.ui.util

import android.content.Context
import android.content.res.Configuration

fun Context.getWindowBackgroundColor(): Int {
    val array = theme.obtainStyledAttributes(
        intArrayOf(android.R.attr.windowBackground)
    )
    val windowBackground = array.getColor(0, 0xFF00FF)
    array.recycle()
    return windowBackground
}

fun Context.isNightMode(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}