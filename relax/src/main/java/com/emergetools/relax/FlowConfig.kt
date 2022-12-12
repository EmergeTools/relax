package com.emergetools.relax

data class FlowConfig(
    val debug: Boolean = false,
    val trace: Boolean = false,
    /** In milliseconds */
    val waitForIdleTimeout: Long = 10_000,
    val scrollToMaxSwipes: Int = 30,
    val swipeSteps: Int = 100,
    val errorHandler: FlowErrorHandler = DefaultFlowErrorHandler,
    /** In milliseconds */
    val launchTimeout: Long = 5_000,
    /** In milliseconds */
    val reportFullyDrawnTimeout: Long = 10_000
)
