package com.emergetools.relax.internal

interface FlowInterceptor {
    fun <T> intercept(event: String, call: () -> T): T
}
