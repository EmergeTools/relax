package com.emergetools.relaxtests

import android.widget.Button
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiObjectNotFoundException
import com.emergetools.relax.Relax
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClickTest {

    @Test
    fun click() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            click("NEXT")
            click("id/button_second")
            click(Button::class.java)
            click {
                textMatches("P.+")
            }
        }
    }

    @Test
    fun clickObjectDoesntExist() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            assertThrows(UiObjectNotFoundException::class.java) {
                click("DOESN'T EXIST")
            }
        }
    }

    @Test
    fun optionalClick() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            optional {
                click("DOESN'T EXIST")
            }
        }
    }
}
