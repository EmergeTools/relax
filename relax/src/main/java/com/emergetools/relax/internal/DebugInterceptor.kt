package com.emergetools.relax.internal

import android.util.Log
import com.emergetools.relax.Relax

object DebugInterceptor : FlowInterceptor {
    override fun <T> intercept(event: String, call: () -> T): T {
        Log.d(Relax.TAG, event)
        return call()
    }
}
