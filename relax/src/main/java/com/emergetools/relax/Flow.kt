package com.emergetools.relax

import android.app.Instrumentation
import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.emergetools.relax.internal.DebugInterceptor
import com.emergetools.relax.internal.TracingInterceptor
import org.junit.Assert
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class Flow(val packageName: String, val config: FlowConfig) {

    val appContext: Context get() = ApplicationProvider.getApplicationContext()

    val instrumentation: Instrumentation get() = InstrumentationRegistry.getInstrumentation()

    val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    val uiAutomation: UiAutomation
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                instrumentation.getUiAutomation(Configurator.getInstance().uiAutomationFlags)
            } else {
                instrumentation.uiAutomation
            }
        }

    private val interceptors = listOfNotNull(
        if (config.debug) DebugInterceptor else null,
        if (config.trace) TracingInterceptor else null
    )

    fun optional(flow: Flow.() -> Unit) = intercept {
        Flow(packageName, config.copy(errorHandler = NoOpFlowErrorHandler)).flow()
    }

    /**
     * Waits for the accessibility event stream to become idle. If the timeout is reached the
     * function returns normally with no errors.
     *
     * @param timeout in milliseconds, default to the config value
     */
    fun waitForIdle(timeout: Long = config.waitForIdleTimeout) = intercept {
        device.waitForIdle(timeout)
    }

    fun waitForReportFullyDrawn(
        timeout: Long = config.reportFullyDrawnTimeout,
        unit: TimeUnit = TimeUnit.MILLISECONDS,
        block: () -> Unit
    ) {
        uiAutomation.executeShellCommand("timeout ${unit.toSeconds(timeout)} logcat -T 1 -v raw -s ActivityTaskManager:I").use { fileDescriptor ->
            ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor).use { inputStream ->
                val executor = Executors.newSingleThreadExecutor()
                val future = executor.submit {
                    inputStream.bufferedReader().lineSequence()
                        .find { it.startsWith("Fully drawn $packageName") }
                    inputStream.close()
                }

                block()

                try {
                    future.get()
                } catch (e: TimeoutException) {
                    config.errorHandler.onError(e)
                } finally {
                    executor.shutdownNow()
                }
            }
        }
    }

    /**
     * Simulates a short press on the HOME button and waits for the system launcher to appear.
     *
     * @return true if successful, false otherwise
     */
    fun pressHome(): Boolean = intercept {
        device.pressHome().also { result ->
            if (!result) error(UnsupportedOperationException("pressHome"))
            waitForlaunch(device.launcherPackageName)
        }
    }

    /**
     * Simulates a short press on the BACK button and waits for the application to become idle.
     *
     * @return true if successful, false otherwise
     */
    fun pressBack(): Boolean = intercept {
        device.pressBack().also { result ->
            if (!result) error(UnsupportedOperationException("pressBack"))
            waitForIdle()
        }
    }

    /**
     * Force-stops the application. Requires using a "com.android.test" module.
     *
     * @param packageName the package name of the application to force-stop, defaults to the flow's
     *   package name
     */
    fun forceStop(packageName: String = this.packageName) = intercept {
        device.executeShellCommand("am force-stop $packageName")
    }

    /**
     * Deletes all data associated with the package. Requires using a "com.android.test" module.
     *
     * @param packageName the package name of the application to clear, defaults to the flow's
     *   package name
     */
    fun clearData(packageName: String = this.packageName) = intercept {
        device.executeShellCommand("pm clear $packageName")
    }

    /**
     * Launches the application and waits for it to appear.
     *
     * @param packageName the package name of the application to launch, defaults to the flow's
     *   package name
     */
    fun launch(packageName: String = this.packageName) = intercept {
        val intent =
            checkNotNull(appContext.packageManager.getLaunchIntentForPackage(packageName)) {
                "Could not find launch intent for app $packageName"
            }
        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)

        waitForlaunch(packageName)
    }

    /**
     * Launches the application using a link and waits for it to appear.
     */
    fun launchWithLink(link: String) = intercept {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(link)

        // Clear out any previous instances
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)

        waitForlaunch()
    }

    /**
     * "Cold" launches the application and waits for it to appear. This is a shortcut for
     * force-stopping the application and launching it again. Requires using a "com.android.test"
     * module.
     */
    fun coldLaunch(packageName: String = this.packageName) = intercept {
        forceStop(packageName)
        launch(packageName)
    }

    /**
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return A UiObject which represents a view that matches the criteria.
     */
    fun findObject(textOrResId: String) = findObject {
        textOrResId(textOrResId)
    }

    /**
     * If the object exists, calls [block] on it and returns its return value, otherwise returns
     * [default].
     */
    private fun <T> findObject(
        select: RelaxSelector.() -> Unit,
        default: T,
        block: (UiObject) -> T
    ): T {
        val obj = findObject(select)
        return if (obj.exists()) block(obj) else default
    }

    /**
     * @return A UiObject which represents a view that matches the criteria.
     */
    fun findObject(select: RelaxSelector.() -> Unit): UiObject = intercept {
        val selector = RelaxSelector(packageName).apply(select).toUiSelector()
        device.findObject(selector).also {
            if (!it.exists()) error(UiObjectNotFoundException(it.selector.toString()))
        }
    }

    /**
     * Performs a click at the center of the visible bounds of the UI element that matches the
     * criteria.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if successful, false otherwise
     */
    fun click(textOrResId: String): Boolean = click { textOrResId(textOrResId) }

    /**
     * Performs a click at the center of the visible bounds of the UI element that matches the
     * criteria.
     *
     * @return true if successful, false otherwise
     */
    fun click(type: Class<*>): Boolean = click { className(type) }

    /**
     * Performs a click at the center of the visible bounds of the UI element that matches the
     * criteria.
     *
     * @return true if successful, false otherwise
     */
    fun click(select: RelaxSelector.() -> Unit): Boolean = intercept {
        findObject(select, false) {
            it.click().also { result ->
                if (!result) error(UnsupportedOperationException("click ${it.selector}"))
                waitForIdle()
            }
        }
    }

    /**
     * Performs a click at arbitrary coordinates.
     *
     * @return true if successful, false otherwise
     */
    fun click(x: Int, y: Int): Boolean = intercept {
        device.click(x, y).also { result ->
            if (!result) error(UnsupportedOperationException("click [x=$x, y=$y]"))
            waitForIdle()
        }
    }

    /**
     * Performs a long click at the center of the visible bounds of the UI element that matches the
     * criteria.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if successful, false otherwise
     */
    fun longClick(textOrResId: String): Boolean = longClick {
        textOrResId(textOrResId)
    }

    /**
     * Performs a long click at the center of the visible bounds of the UI element that matches the
     * criteria.
     *
     * @return true if successful, false otherwise
     */
    fun longClick(type: Class<*>): Boolean = longClick { className(type) }

    /**
     * Performs a long click at the center of the visible bounds of the UI element that matches the
     * criteria.
     *
     * @return true if successful, false otherwise
     */
    fun longClick(select: RelaxSelector.() -> Unit): Boolean = intercept {
        findObject(select, false) {
            it.longClick().also { result ->
                if (!result) error(UnsupportedOperationException("longClick ${it.selector}"))
                waitForIdle()
            }
        }
    }

    /**
     * Sets the text in an editable field, after clearing the field's content. The search criteria
     * must reference a UI element that is editable.
     *
     * @param inputText the text to set
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if successful, false otherwise
     */
    fun inputText(inputText: String, textOrResId: String) = inputText(inputText) {
        textOrResId(textOrResId)
    }

    /**
     * Sets the text in an editable field, after clearing the field's content. The search criteria
     * must reference a UI element that is editable.
     *
     * @param inputText the text to set
     * @return true if successful, false otherwise
     */
    fun inputText(inputText: String, type: Class<*>) = inputText(inputText) {
        className(type)
    }

    /**
     * Sets the text in an editable field, after clearing the field's content. The search criteria
     * must reference a UI element that is editable.
     *
     * @param inputText the text to set
     * @return true if successful, false otherwise
     */
    fun inputText(inputText: String, select: RelaxSelector.() -> Unit): Boolean = intercept {
        findObject(select, false) {
            it.setText(inputText).also { result ->
                if (!result) error(UnsupportedOperationException("inputText $inputText ${it.selector}"))
                waitForIdle()
            }
        }
    }

    /**
     * @param text Searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return UiScrollable instance that meets the criteria
     */
    fun scrollable(textOrResId: String): UiScrollable {
        return scrollable { textOrResId(textOrResId) }
    }

    /**
     * @return UiScrollable instance that meets the criteria
     */
    fun scrollable(type: Class<*>): UiScrollable {
        return scrollable { className(type) }
    }

    /**
     * If the object exists, calls [block] on it and returns its return value, otherwise returns
     * [default].
     */
    private fun <T> scrollable(
        select: RelaxSelector.() -> Unit,
        default: T,
        block: (UiScrollable) -> T
    ): T {
        val obj = scrollable(select)
        return if (obj.exists()) block(obj) else default
    }

    /**
     * @return UiScrollable instance that meets the criteria
     */
    fun scrollable(select: RelaxSelector.() -> Unit): UiScrollable {
        val selector = RelaxSelector(packageName).apply(select).toUiSelector()
        return UiScrollable(selector).also {
            if (!it.exists()) error(UiObjectNotFoundException(it.selector.toString()))
        }
    }

    /**
     * Performs a forward scroll. If the swipe direction is set to vertical, then the swipes will be
     * performed from bottom to top. If the swipe direction is set to horizontal, then the swipes
     * will be performed from right to left. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollForward(textOrResId: String): Boolean = scrollForward {
        textOrResId(textOrResId)
    }

    /**
     * Performs a forward scroll. If the swipe direction is set to vertical, then the swipes will be
     * performed from bottom to top. If the swipe direction is set to horizontal, then the swipes
     * will be performed from right to left. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollForward(type: Class<*>): Boolean = scrollForward {
        className(type)
    }

    /**
     * Performs a forward scroll. If the swipe direction is set to vertical, then the swipes will be
     * performed from bottom to top. If the swipe direction is set to horizontal, then the swipes
     * will be performed from right to left. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollForward(select: RelaxSelector.() -> Unit): Boolean = intercept {
        scrollable(select, false) {
            it.scrollForward().also { result ->
                if (!result) error(UnsupportedOperationException("scrollForward ${it.selector}"))
            }
        }
    }

    /**
     * Performs a backward scroll. If the swipe direction is set to vertical, then the swipes will
     * be performed from top to bottom. If the swipe direction is set to horizontal, then the swipes
     * will be performed from left to right. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollBackward(textOrResId: String): Boolean = scrollBackward {
        textOrResId(textOrResId)
    }

    /**
     * Performs a backward scroll. If the swipe direction is set to vertical, then the swipes will
     * be performed from top to bottom. If the swipe direction is set to horizontal, then the swipes
     * will be performed from left to right. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollBackward(type: Class<*>): Boolean = scrollBackward {
        className(type)
    }

    /**
     * Performs a backward scroll. If the swipe direction is set to vertical, then the swipes will
     * be performed from top to bottom. If the swipe direction is set to horizontal, then the swipes
     * will be performed from left to right. Make sure to take into account devices configured with
     * right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollBackward(select: RelaxSelector.() -> Unit): Boolean = intercept {
        scrollable(select, false) {
            it.scrollBackward().also { result ->
                if (!result) error(UnsupportedOperationException("scrollBackward ${it.selector}"))
            }
        }
    }

    /**
     * Scrolls to the end of a scrollable layout element. The end can be at the bottom-most edge in
     * the case of vertical controls, or the right-most edge for horizontal controls. Make sure to
     * take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollToEnd(
        maxSwipes: Int = config.scrollToMaxSwipes,
        textOrResId: String
    ): Boolean = scrollToEnd(maxSwipes) { textOrResId(textOrResId) }


    /**
     * Scrolls to the end of a scrollable layout element. The end can be at the bottom-most edge in
     * the case of vertical controls, or the right-most edge for horizontal controls. Make sure to
     * take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollToEnd(
        maxSwipes: Int = config.scrollToMaxSwipes,
        type: Class<*>
    ): Boolean = scrollToEnd(maxSwipes) { className(type) }

    /**
     * Scrolls to the end of a scrollable layout element. The end can be at the bottom-most edge in
     * the case of vertical controls, or the right-most edge for horizontal controls. Make sure to
     * take into account devices configured with right-to-left languages like Arabic and Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollToEnd(
        maxSwipes: Int = config.scrollToMaxSwipes,
        select: RelaxSelector.() -> Unit
    ): Boolean = intercept {
        scrollable(select, false) {
            it.scrollToEnd(maxSwipes).also { result ->
                if (!result) error(UnsupportedOperationException("scrollToEnd ${it.selector}"))
            }
        }
    }

    /**
     * Scrolls to the beginning of a scrollable layout element. The beginning can be at the top-most
     * edge in the case of vertical controls, or the left-most edge for horizontal controls. Make
     * sure to take into account devices configured with right-to-left languages like Arabic and
     * Hebrew.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollToBeginning(
        maxSwipes: Int = config.scrollToMaxSwipes,
        textOrResId: String
    ): Boolean = scrollToBeginning(maxSwipes) { textOrResId(textOrResId) }

    /**
     * Scrolls to the beginning of a scrollable layout element. The beginning can be at the top-most
     * edge in the case of vertical controls, or the left-most edge for horizontal controls. Make
     * sure to take into account devices configured with right-to-left languages like Arabic and
     * Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollToBeginning(
        maxSwipes: Int = config.scrollToMaxSwipes,
        type: Class<*>
    ): Boolean = scrollToBeginning(maxSwipes) { className(type) }

    /**
     * Scrolls to the beginning of a scrollable layout element. The beginning can be at the top-most
     * edge in the case of vertical controls, or the left-most edge for horizontal controls. Make
     * sure to take into account devices configured with right-to-left languages like Arabic and
     * Hebrew.
     *
     * @return true if scrolled, false if can't scroll anymore
     */
    fun scrollToBeginning(
        maxSwipes: Int = config.scrollToMaxSwipes,
        select: RelaxSelector.() -> Unit
    ): Boolean = intercept {
        scrollable(select, false) {
            it.scrollToBeginning(maxSwipes).also { result ->
                if (!result) error(UnsupportedOperationException("scrollToBeginning ${it.selector}"))
            }
        }
    }

    /**
     * Performs the swipe down action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if successful, false otherwise
     */
    fun swipeDown(textOrResId: String): Boolean = swipeDown { textOrResId(textOrResId) }

    /**
     * Performs the swipe down action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @return true if successful, false otherwise
     */
    fun swipeDown(type: Class<*>): Boolean = swipeDown { className(type) }

    /**
     * Performs the swipe down action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @return true if successful, false otherwise
     */
    fun swipeDown(select: RelaxSelector.() -> Unit): Boolean = intercept {
        findObject(select, false) {
            it.swipeDown(config.swipeSteps).also { result ->
                if (!result) error(UnsupportedOperationException("swipeDown ${it.selector}"))
            }
        }
    }

    /**
     * Performs the swipe left action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if successful, false otherwise
     */
    fun swipeLeft(textOrResId: String): Boolean = swipeLeft { textOrResId(textOrResId) }

    /**
     * Performs the swipe left action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @return true if successful, false otherwise
     */
    fun swipeLeft(type: Class<*>): Boolean = swipeLeft { className(type) }

    /**
     * Performs the swipe left action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @return true if successful, false otherwise
     */
    fun swipeLeft(select: RelaxSelector.() -> Unit): Boolean = intercept {
        findObject(select, false) {
            it.swipeLeft(config.swipeSteps).also { result ->
                if (!result) error(UnsupportedOperationException("swipeLeft ${it.selector}"))
            }
        }
    }

    /**
     * Performs the swipe right action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if successful, false otherwise
     */
    fun swipeRight(textOrResId: String): Boolean = swipeRight { textOrResId(textOrResId) }

    /**
     * Performs the swipe right action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @return true if successful, false otherwise
     */
    fun swipeRight(type: Class<*>): Boolean = swipeRight { className(type) }

    /**
     * Performs the swipe right action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @return true if successful, false otherwise
     */
    fun swipeRight(select: RelaxSelector.() -> Unit): Boolean = intercept {
        findObject(select, false) {
            it.swipeRight(config.swipeSteps).also { result ->
                if (!result) error(UnsupportedOperationException("swipeRight ${it.selector}"))
            }
        }
    }

    /**
     * Performs the swipe up action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @param textOrResId If the string contains ":id/" it will be treated as a resource ID,
     *   otherwise searches for the element whose visible text matches exactly. Matching is
     *   case-sensitive.
     * @return true if successful, false otherwise
     */
    fun swipeUp(textOrResId: String): Boolean = swipeUp { textOrResId(textOrResId) }

    /**
     * Performs the swipe up action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @return true if successful, false otherwise
     */
    fun swipeUp(type: Class<*>): Boolean = swipeUp { className(type) }

    /**
     * Performs the swipe up action on the UiObject. The swipe gesture can be performed over any
     * surface. The targeted UI element does not need to be scrollable.
     *
     * @return true if successful, false otherwise
     */
    fun swipeUp(select: RelaxSelector.() -> Unit): Boolean = intercept {
        findObject(select, false) {
            it.swipeUp(config.swipeSteps).also { result ->
                if (!result) error(UnsupportedOperationException("swipeUp ${it.selector}"))
            }
        }
    }

    fun assertExists(textOrResId: String) = assertExists { textOrResId(textOrResId) }

    fun assertExists(type: Class<*>) = assertExists { className(type) }

    fun assertExists(select: RelaxSelector.() -> Unit) = intercept {
        Assert.assertTrue(findObject(select).exists())
    }

    fun assertNotExists(textOrResId: String) = assertNotExists { textOrResId(textOrResId) }

    fun assertNotExists(type: Class<*>) = assertNotExists { className(type) }

    fun assertNotExists(select: RelaxSelector.() -> Unit) = intercept {
        Assert.assertFalse(findObject(select).exists())
    }

    private fun <T> intercept(call: () -> T): T {
        val caller = Thread.currentThread().stackTrace
            .firstOrNull {
                it.className == Flow::class.java.canonicalName &&
                        it.methodName != "intercept"
            }
            ?.methodName
            ?: "unknown"

        return interceptors.fold(call) { acc, interceptor ->
            { interceptor.intercept(caller, acc) }
        }.invoke()
    }

    private fun waitForlaunch(packageName: String = this.packageName) {
        device.wait(
            Until.hasObject(By.pkg(packageName).depth(0)),
            config.launchTimeout
        )
            ?: error(TimeoutException("Launch of $packageName timed out after ${config.launchTimeout}ms"))
    }

    private fun error(e: Throwable) {
        config.errorHandler.onError(e)
    }
}
