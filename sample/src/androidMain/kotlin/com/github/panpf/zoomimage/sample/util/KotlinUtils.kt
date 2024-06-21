package com.github.panpf.zoomimage.sample.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> T.letIf(predicate: Boolean, block: (T) -> T): T {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }
    return if (predicate) block(this) else this
}

fun <T> Flow<T>.ignoreFirst(): Flow<T> {
    var first = true
    return filter {
        if (first) {
            first = false
            false
        } else {
            true
        }
    }
}