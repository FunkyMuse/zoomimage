package com.github.panpf.zoomimage.sample.ui.util.view

import android.view.View
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.github.panpf.zoomimage.sample.R

internal val View.lifecycleOwner: LifecycleOwner
    get() {
        synchronized(this) {
            val tag = getTag(R.id.tagId_viewLifecycle)
            if (tag != null && tag is ViewLifecycleOwner) {
                return tag
            } else {
                val viewLifecycleOwner = ViewLifecycleOwner(this)
                setTag(R.id.tagId_viewLifecycle, viewLifecycleOwner)
                return viewLifecycleOwner
            }
        }
    }

internal class ViewLifecycleOwner(view: View) : LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }

            override fun onViewDetachedFromWindow(v: View) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            }
        })

        if (ViewCompat.isAttachedToWindow(view)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}