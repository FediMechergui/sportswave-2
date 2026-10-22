package com.sportswave.app

/** Re-resolve the original channel after a failed CDN redirect, with a bounded delay. */
object ProviderRecovery {
    fun shouldRetry(httpStatus: Int?, lostLiveWindow: Boolean, attempts: Int, networkUnavailable: Boolean = false): Boolean =
        attempts < 2 && (lostLiveWindow || networkUnavailable || httpStatus in setOf(401,403,404,410,502,503))
    fun delayMs(attempt: Int): Long = if(attempt<=1) 1000L else 2000L
}
