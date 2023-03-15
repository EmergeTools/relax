package com.emergetools.relaxtests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emergetools.relax.FlowConfig
import com.emergetools.relax.Relax
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutoEmergeTest {

    @Test
    fun install() {
        Relax("com.android.vending", FlowConfig(debug = true)) {
            launchWithLink("https://play.google.com/store/apps/details?id=org.thoughtcrime.securesms")

            if (findObject { description("Open") }.exists()) return@Relax

            getObject { description("Install") }.click()

            val installing = findObject("Installing…")
            installing.waitForExists(60_000)
            installing.waitUntilGone(60_000)
        }
    }
}