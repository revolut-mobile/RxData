package com.revolut.rxdata.core

/*
 * Copyright (C) 2019 Revolut
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

data class Data<out T>(
    val content: T? = null,
    val error: Throwable? = null,
    val loading: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Data<*>

        if (content != other.content) return false
        if (error?.javaClass != other.error?.javaClass) return false
        if (error?.message != other.error?.message) return false
        if (loading != other.loading) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content?.hashCode() ?: 0
        result = 31 * result + (error?.javaClass?.hashCode() ?: 0)
        result = 31 * result + (error?.message?.hashCode() ?: 0)
        result = 31 * result + loading.hashCode()
        return result
    }
}