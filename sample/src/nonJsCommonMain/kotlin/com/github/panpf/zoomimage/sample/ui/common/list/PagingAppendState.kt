package com.github.panpf.zoomimage.sample.ui.common.list

import androidx.compose.runtime.Composable
import app.cash.paging.LoadState
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.LoadStateNotLoading
import com.github.panpf.zoomimage.sample.ui.components.AppendState

@Composable
fun PagingAppendState(loadState: LoadState, onClick: () -> Unit) {
    val state = when (loadState) {
        is LoadStateLoading -> AppendState.LOADING
        is LoadStateError -> AppendState.ERROR
        is LoadStateNotLoading -> if (loadState.endOfPaginationReached) AppendState.END else AppendState.LOADING
        else -> AppendState.LOADING
    }
    AppendState(state, onClick)
}