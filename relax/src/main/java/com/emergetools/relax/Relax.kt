package com.emergetools.relax

object Relax {

    internal val TAG = "Relax"

    fun flow(packageName: String, config: FlowConfig = FlowConfig(), flow: Flow.() -> Unit) {
        Flow(packageName, config).flow()
    }
}
