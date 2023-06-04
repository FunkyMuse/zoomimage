package com.github.panpf.zoom.sample.ui.myview

import android.app.Application
import android.widget.ImageView.ScaleType
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.github.panpf.zoom.ZoomImageView
import com.github.panpf.zoom.sample.prefsService
import com.github.panpf.zoom.sample.util.lifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class SettingsEventViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsService = application.prefsService

    val listRestartImageFlow: Flow<Any> = merge(
        prefsService.saveCellularTrafficInList.sharedFlow,

        prefsService.resizePrecision.sharedFlow,
        prefsService.resizeScale.sharedFlow,
        prefsService.longImageResizeScale.sharedFlow,
        prefsService.otherImageResizeScale.sharedFlow,

        prefsService.bitmapQuality.sharedFlow,
        prefsService.colorSpace.sharedFlow,
        prefsService.inPreferQualityOverSpeed.sharedFlow,

        prefsService.disabledMemoryCache.sharedFlow,
        prefsService.disabledResultCache.sharedFlow,
        prefsService.disabledDownloadCache.sharedFlow,
        prefsService.disallowReuseBitmap.sharedFlow,

        prefsService.disallowAnimatedImageInList.sharedFlow,
    )

    val listReloadFlow: SharedFlow<Any> = prefsService.ignoreExifOrientation.sharedFlow

//    fun observeListSettings(recyclerView: MyRecyclerView) {
//        recyclerView.lifecycleOwner.lifecycleScope.launch {
//            listRestartImageFlow.collect {
//                recyclerView.descendants.forEach {
//                    SketchUtils.restart(it)
//                }
//            }
//        }
//
//        recyclerView.lifecycleOwner.lifecycleScope.launch {
//            listReloadFlow.collect {
//                recyclerView.adapter?.findPagingAdapter()?.refresh()
//            }
//        }
//    }

    fun observeZoomSettings(zoomImageView: ZoomImageView) {
        zoomImageView.lifecycleOwner.lifecycleScope.launch {
            prefsService.scrollBarEnabled.stateFlow.collect {
                zoomImageView.zoomAbility.scrollBarEnabled = it
            }
        }
        zoomImageView.lifecycleOwner.lifecycleScope.launch {
            prefsService.readModeEnabled.stateFlow.collect {
                zoomImageView.zoomAbility.readModeEnabled = it
            }
        }
        zoomImageView.lifecycleOwner.lifecycleScope.launch {
            prefsService.showTileBoundsInHugeImagePage.stateFlow.collect {
                zoomImageView.subsamplingAbility.showTileBounds = it
            }
        }
        zoomImageView.lifecycleOwner.lifecycleScope.launch {
            prefsService.scaleType.stateFlow.collect {
                zoomImageView.scaleType = ScaleType.valueOf(it)
            }
        }
    }

    private fun Adapter<*>.findPagingAdapter(): PagingDataAdapter<*, *>? {
        when (this) {
            is PagingDataAdapter<*, *> -> {
                return this
            }

            is ConcatAdapter -> {
                this.adapters.forEach {
                    it.findPagingAdapter()?.let { pagingDataAdapter ->
                        return pagingDataAdapter
                    }
                }
                return null
            }

            else -> {
                return null
            }
        }
    }
}