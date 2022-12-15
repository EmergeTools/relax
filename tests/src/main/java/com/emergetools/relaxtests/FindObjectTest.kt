package com.emergetools.relaxtests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiObjectNotFoundException
import com.emergetools.relax.Relax
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FindObjectTest {

    @Test
    fun findObject() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()
            val obj = findObject("NEXT")

            assertTrue(obj.exists())
        }
    }

    @Test
    fun findObjectDoesntExist() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            assertThrows(UiObjectNotFoundException::class.java) {
                findObject("DOESN'T EXIST")
            }
        }
    }

    @Test
    fun optionalFindObject() {
        Relax("com.emergetools.relaxexamples") {
            pressHome()
            launch()

            optional {
                findObject("DOESN'T EXIST")
            }
        }
    }
}
