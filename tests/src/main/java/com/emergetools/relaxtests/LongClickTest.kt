package com.emergetools.relaxtests

import android.widget.Button
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiObjectNotFoundException
import com.emergetools.relax.Relax
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LongClickTest {

    @Test
    fun longClick() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            longClick("NEXT")
            longClick("id/button_second")
            longClick(Button::class.java)
            longClick {
                textMatches("P.+")
            }
        }
    }

    @Test
    fun longClickObjectDoesntExist() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            assertThrows(UiObjectNotFoundException::class.java) {
                longClick("DOESN'T EXIST")
            }
        }
    }

    @Test
    fun optionalLongClick() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            optional {
                longClick("DOESN'T EXIST")
            }
        }
    }
}
