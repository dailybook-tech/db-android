package com.boilerplate.network

import java.util.*

interface ITimeLimitedResource {
    var refreshRate: Long
    val lastUpdate: Date?

    suspend fun evict(cleanup: Boolean = false)
}