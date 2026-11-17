package com.bookverse.contacts.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

private const val SETUP_ACTIVITY = "com.bookverse.contacts.benchmark.BenchmarkSetupActivity"
private const val SYNTHETIC_CONTACT_COUNT = 2_000
private const val SYNTHETIC_CONTACT_PREFIX = "Benchmark Contact"
private const val ALLOW_PHYSICAL_DEVICE_ARGUMENT = "allowPhysicalDevice"
private const val SETUP_TIMEOUT_MILLIS = 30_000L
private const val UI_TIMEOUT_MILLIS = 5_000L
private const val TAB_CONTACTS = "Contacts"
private const val TAB_FAVORITES = "Favorites"
private const val TAB_HISTORY = "History"
private const val DIALER_DESCRIPTION = "Dialer"
private const val DIALED_DIGITS = "5551234"

internal fun targetPackageName(): String =
    InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: error("The targetAppId instrumentation argument is missing.")

internal fun prepareSyntheticContacts(packageName: String) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    check(device.isEmulator() || physicalDeviceAllowed()) {
        "Synthetic contact setup clears the target app's data. Run it on an emulator, " +
            "or pass -P android.testInstrumentationRunnerArguments.$ALLOW_PHYSICAL_DEVICE_ARGUMENT=true " +
            "for a dedicated test device."
    }
    device.executeShellCommand("pm clear $packageName")
    device.executeShellCommand(
        "am start -W -n $packageName/$SETUP_ACTIVITY --ei contact_count $SYNTHETIC_CONTACT_COUNT",
    )
    checkNotNull(
        device.wait(Until.findObject(By.textStartsWith(SYNTHETIC_CONTACT_PREFIX)), SETUP_TIMEOUT_MILLIS),
    ) { "Synthetic contacts did not appear within $SETUP_TIMEOUT_MILLIS ms." }
    device.executeShellCommand("am force-stop $packageName")
}

internal fun MacrobenchmarkScope.awaitContactsList(): UiObject2 {
    checkNotNull(
        device.wait(Until.findObject(By.textStartsWith(SYNTHETIC_CONTACT_PREFIX)), UI_TIMEOUT_MILLIS),
    ) { "The Contacts list did not render." }
    return checkNotNull(
        device.findObjects(By.scrollable(true)).lastOrNull(),
    ) { "The Contacts list is not scrollable." }
}

internal fun MacrobenchmarkScope.scrollContactsList() {
    val list = awaitContactsList()
    list.setGestureMargins(
        device.displayWidth / 5,
        device.displayHeight / 4,
        device.displayWidth / 5,
        device.displayHeight / 4,
    )
    repeat(3) {
        list.fling(Direction.DOWN)
        device.waitForIdle()
    }
    list.fling(Direction.UP)
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.exerciseCoreJourneys() {
    scrollContactsList()
    openTab(TAB_FAVORITES)
    openTab(TAB_HISTORY)
    openTab(TAB_CONTACTS)
    awaitContactsList()
    openDialer()
}

private fun MacrobenchmarkScope.openTab(label: String) {
    findRequired(By.text(label)).click()
    device.waitForIdle()
}

private fun MacrobenchmarkScope.openDialer() {
    findRequired(By.desc(DIALER_DESCRIPTION)).click()
    device.waitForIdle()
    DIALED_DIGITS.forEach { digit ->
        keypadKey(digit).click()
    }
    device.waitForIdle()
    device.pressBack()
    device.waitForIdle()
}

private fun MacrobenchmarkScope.findRequired(selector: BySelector): UiObject2 =
    checkNotNull(device.wait(Until.findObject(selector), UI_TIMEOUT_MILLIS)) {
        "Expected UI element was not found: $selector"
    }

private fun MacrobenchmarkScope.keypadKey(digit: Char): UiObject2 {
    val selector = By.text(digit.toString())
    findRequired(selector)
    return device.findObjects(selector).maxBy { it.visibleBounds.top }
}

private fun UiDevice.isEmulator(): Boolean =
    listOf("ro.kernel.qemu", "ro.boot.qemu").any { property ->
        executeShellCommand("getprop $property").trim() == "1"
    }

private fun physicalDeviceAllowed(): Boolean =
    InstrumentationRegistry.getArguments().getString(ALLOW_PHYSICAL_DEVICE_ARGUMENT) == "true"
