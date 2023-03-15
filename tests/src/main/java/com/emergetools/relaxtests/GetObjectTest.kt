package com.emergetools.relaxtests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiObjectNotFoundException
import com.emergetools.relax.Relax
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GetObjectTest {

    @Test
    fun getObject() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()
            val obj = getObject("NEXT")

            assertTrue(obj.exists())
        }
    }

    @Test
    fun getObjectDoesntExist() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            assertThrows(UiObjectNotFoundException::class.java) {
                getObject("DOESN'T EXIST")
            }
        }
    }

    @Test
    fun optionalGetObject() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            optional {
                getObject("DOESN'T EXIST")
            }
        }
    }
}
