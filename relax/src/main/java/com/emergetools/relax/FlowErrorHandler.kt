package com.emergetools.relax

interface FlowErrorHandler {
    fun onError(error: Throwable)
}

object NoOpFlowErrorHandler : FlowErrorHandler {
    override fun onError(error: Throwable) {
        // Do nothing.
    }
}

object DefaultFlowErrorHandler : FlowErrorHandler {
    override fun onError(error: Throwable) {
        throw error
    }
}
