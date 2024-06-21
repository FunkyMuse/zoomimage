package com.github.panpf.zoomimage.sample.ui

import androidx.compose.runtime.Composable
import com.github.panpf.zoomimage.sample.ui.model.ImageResource
import com.github.panpf.zoomimage.sample.ui.navigation.Navigation
import com.github.panpf.zoomimage.sample.ui.screen.GalleryScreen
import com.github.panpf.zoomimage.sample.ui.screen.MainScreen
import com.github.panpf.zoomimage.sample.ui.screen.SlideshowScreen

sealed interface Page {

    @Composable
    fun content(navigation: Navigation, index: Int)

    data object Main : Page {

        @Composable
        override fun content(navigation: Navigation, index: Int) {
            MainScreen(navigation)
        }
    }

    data object Gallery : Page {

        @Composable
        override fun content(navigation: Navigation, index: Int) {
            GalleryScreen(navigation)
        }
    }

    data class Slideshow(val imageResources: List<ImageResource>, val currentIndex: Int) : Page {

        @Composable
        override fun content(navigation: Navigation, index: Int) {
            SlideshowScreen(navigation, imageResources, currentIndex)
        }
    }
}