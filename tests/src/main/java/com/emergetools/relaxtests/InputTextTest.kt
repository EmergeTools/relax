package com.emergetools.relaxtests

import android.widget.EditText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObjectNotFoundException
import com.emergetools.relax.Relax
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputTextTest {

    @Test
    fun inputText() {
        Relax.flow("com.emergetools.relaxexamples") {
            pressHome()
            launch()
            click("NEXT")
            inputText("Felix", EditText::class.java)

            assertNotNull(device.findObject(By.text("Felix")))
        }
    }

    @Test
    fun inputTextObjectDoesntExist() {
        Relax.flow("com.emergetools.relaxexamples") {
            pressHome()
            launch()
            click("NEXT")

            assertThrows(UiObjectNotFoundException::class.java) {
                inputText("Felix", "DOESN'T EXIST")
            }
        }
    }

    @Test
    fun optionalInputText() {
        Relax.flow("com.emergetools.relaxexamples") {
            pressHome()
            launch()
            click("NEXT")

            optional {
                inputText("Felix", "DOESN'T EXIST")
            }
        }
    }
}
