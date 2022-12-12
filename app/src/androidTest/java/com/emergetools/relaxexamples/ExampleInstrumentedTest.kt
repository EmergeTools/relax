package com.emergetools.relaxexamples

import android.widget.Button
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emergetools.relax.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun test() {
        val config = FlowConfig(debug = true)
        Relax.flow("com.emergetools.relaxexamples", config) {
            pressHome()
            launch()
            click("id/fab")
            click(Button::class.java)
            optional {
                scrollForward("Hello first fragment")
                click("Doesn't exist")
            }
            click {
                classNameMatches(".+\\.Button")
                textStartsWith("N")
            }
            assertExists {
                enabled(true)
                scrollable(false)
                text("PREVIOUS")
            }
        }
    }
}