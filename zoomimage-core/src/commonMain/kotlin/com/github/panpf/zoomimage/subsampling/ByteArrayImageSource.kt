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

package com.github.panpf.zoomimage.subsampling

import okio.Buffer
import okio.Source

/**
 * Create an image source from a ByteArray.
 */
fun ImageSource.Companion.fromByteArray(byteArray: ByteArray): ByteArrayImageSource {
    return ByteArrayImageSource(byteArray)
}

class ByteArrayImageSource(val byteArray: ByteArray) : ImageSource {

    override val key: String = byteArray.toString()

    override fun openSource(): Source {
        return Buffer().write(byteArray)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ByteArrayImageSource) return false
        if (!byteArray.contentEquals(other.byteArray)) return false
        return true
    }

    override fun hashCode(): Int {
        return byteArray.hashCode()
    }

    override fun toString(): String {
        return "ByteArrayImageSource('$byteArray')"
    }

//    class Factory(val byteArray: ByteArray) : ImageSource.Factory {
//
//        override val key: String = byteArray.toString()
//
//        override suspend fun create(): ByteArrayImageSource {
//            return ByteArrayImageSource(byteArray)
//        }
//
//        override fun equals(other: Any?): Boolean {
//            if (this === other) return true
//            if (other !is Factory) return false
//            if (!byteArray.contentEquals(other.byteArray)) return false
//            return true
//        }
//
//        override fun hashCode(): Int {
//            return byteArray.hashCode()
//        }
//
//        override fun toString(): String {
//            return "ByteArrayImageSource.Factory('$byteArray')"
//        }
//    }
}