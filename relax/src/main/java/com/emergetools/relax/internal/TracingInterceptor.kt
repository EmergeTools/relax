package com.emergetools.relax.internal

import android.os.Trace


object TracingInterceptor : FlowInterceptor {

    private val PREFIX = "relax."

    override fun <T> intercept(event: String, call: () -> T): T {
        Trace.beginSection("$PREFIX$event")
        val result = call()
        Trace.endSection()
        return result
    }
}
