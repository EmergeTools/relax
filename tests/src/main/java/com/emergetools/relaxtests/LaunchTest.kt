package com.emergetools.relaxtests

import android.content.Intent
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import com.emergetools.relax.Relax
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchTest {

    @Test
    fun launch() {
        Relax.flow("com.emergetools.relaxexamples") {
            pressHome()
            waitForReportFullyDrawn {
                launch()
            }
//            uiAutomation.executeShellCommand("logcat")

//            assertNotNull(device.findObject(By.pkg(packageName)))
        }
    }

    @Test
    fun launchArbitraryPackage() {
        Relax.flow("com.emergetools.relaxexamples") {
            pressHome()
            val intent = Intent(Settings.ACTION_SETTINGS)

            @Suppress("DEPRECATION")
            val resolveInfos = appContext.packageManager.queryIntentActivities(intent, MATCH_DEFAULT_ONLY)
            val settingsPackageName = resolveInfos.first().activityInfo.packageName
            launch(settingsPackageName)

            assertNotNull(device.findObject(By.pkg(settingsPackageName)))
        }
    }

    @Test
    fun launchWithLink() {
        Relax.flow("com.emergetools.relaxexamples") {
            pressHome()
            launchWithLink("emerge://deep")

            assertNotNull(device.findObject(By.pkg(packageName)))
        }
    }

    @Test
    fun coldLaunch() {
        Relax.flow("com.emergetools.relaxexamples") {
            pressHome()
            coldLaunch()

            assertNotNull(device.findObject(By.pkg(packageName)))
        }
    }
}
