# Relax

Relax is a lightweight wrapper around Android's UI Automator library. It helps write clear and concise UI tests:

```kotlin
Relax.flow("com.example.myapp") {
    pressHome()
    launch()
    inputText("me@example.com", "id/email")
    inputText("v3ry_s3cur3", "id/password")
    click("Login")
    assertExists {
        textStartsWith("Welcome")
    }
}
```

Rather than:

```kotlin
val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
device.pressHome()
device.wait(Until.hasObject(By.pkg(device.launcherPackageName).depth(0)), 5_000)

val context = ApplicationProvider.getApplicationContext<Context>()
val packageName = "com.example.myapp"
val intent = context.packageManager.getLaunchIntentForPackage(packageName)!!
intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
context.startActivity(intent)
device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 5_000)

device.findObject(UiSelector().resourceId("$packageName:id/email")).text = "me@example.com"
device.findObject(UiSelector().resourceId("$packageName:id/password")).text = "v3ry_s3cur3"
device.findObject(UiSelector().text("Login")).click()
device.waitForIdle()

assertTrue(device.findObject(UiSelector().textStartsWith("Welcome")).exists())
```

## Installation

In your project's `build.gradle`:

```kotlin
dependencies {
    androidTestImplementation("com.emergetools.test:relax:0.1.0")
    // or use implementation if in test module
}
```

**⚠️ Relax is currently in an experimental state and is subject to breaking changes.**

## Getting Started

Relax allows you to build UI test flows as a sequence of actions:

```kotlin
Relax.flow("<your app's package name>") {
    // Arbitrary actions
}
```

## Application Management

Launch the application:
```kotlin
launch()
```

Using a deeplink:
```kotlin
launchWithLink("custom://…")
```

Force-stop the application (requires `com.android.test` module):
```kotlin
forceStop()
```

Clear all data associated with the application package (requires `com.android.test` module):
```kotlin
clearData()
```

## Click

By exact text:
```kotlin
click("Text")
```

By id:
```kotlin
click("id/my_view_id")
```

By class:
```kotlin
click(Button::class.java)
```

Comprehensive selector:
```kotlin
click {
    textContains("Milk")
    checkable(true)
    // …
}
```

On specific coordinates:
```kotlin
click(x, y)
```

To long click simply use `longClick` instead of `click`.

## Input Text

```kotlin
inputText("Ob-la-di", "Input Label")
inputText("ob-la-da", "id/input")
inputText("life goes on", EditText::class.java)
inputText("brah") {
    // Arbitrary selectors
}
```

## Scroll

```kotlin
scrollForward("Text")
scrollBackward("id/list")
scrollToBeginning {
    // Arbitrary selectors
}
scrollToEnd(maxSwipes = 2, "id/list")
```

## Swipe

```kotlin
swipeUp("Text")
swipeDown("id/carousel")
swipeLeft(CarouselView::class.java)
swipeRight {
    // Arbitrary selectors
}
```

## System Buttons

```kotlin
pressHome()
pressBack()
```

## Selectors

The same selectors as [`UiSelector`](https://developer.android.com/reference/androidx/test/uiautomator/UiSelector) are available:

```kotlin
click { // Or any other action that accepts selectors
    checkable(true)
    checked(true)
    child {
        // Arbitrary child selectors
    }
    className(MyView::class.java)
    className("com.example.myapp.MyView")
    classNameMatches("regex")
    description("description")
    descriptionContains("cript")
    descriptionMatches("regex")
    descriptionStartsWith("desc")
    enabled(true)
    focusable(true)
    focused(true)
    index(0)
    instance(0)
    longClickable(true)
    packageName("com.example.myapp")
    packageNameMatches("regex")
    parent {
        // Arbitrary parent selectors
    }
    resId("my_id")
    resIdMatches("regex")
    scrollable(true)
    selected(true)
    text("Text")
    textContains("x")
    textMatches("regex")
    textStartsWith("Te")
}
```

## Assert

```kolint
assertExists("Text")
assertExists("id/my_view")
assertNotExists(MyView::class.java)
assertNotExists {
    // Arbitrary selectors
}
```

## Handle Errors

By default if an object cannot be found or an action cannot be completed the test will fail. This behavior can be easily modified.

Making some actions optional:
```kotlin
optional {
    inputText("SAVE10", "Coupon")
}
click("Purchase")
```

Ignoring all failures by default:
```kotlin
val config = FlowConfig(errorHandler = NoOpFlowErrorHandler)
Relax.flow("id", config) {
    // …
}
```

Custom error handler:
```kotlin
val errorHandler = object : FlowErrorHandler {
    override fun onError(error: Throwable) {
        // …
    }
}
val config = FlowConfig(errorHandler = errorHandler)
Relax.flow("id", config) {
    // …
}
```

## UI Automator Interoperability

Relax is a lightweight wrapper around UI Automator, therefore it's easy to leverage the full feature set of UI Automator. For example:

```kotlin
Relax.flow(/* … */) {
    if (!device.isScreenOn) device.wakeUp()
    launch()
    click("Next")
    device.takeScreenshot(storePath)
    // …
}
```
