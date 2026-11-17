package com.bookverse.contacts.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val STARTUP_ITERATIONS = 10
private const val SCROLL_ITERATIONS = 5

@RunWith(AndroidJUnit4::class)
class CoreBenchmarks {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val packageName = targetPackageName()

    @Before
    fun prepare() {
        prepareSyntheticContacts(packageName)
    }

    @Test
    fun coldStartupWithoutProfile() = coldStartup(CompilationMode.None())

    @Test
    fun coldStartupWithBaselineProfile() = coldStartup(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun contactsScrollWithoutProfile() = contactsScroll(CompilationMode.None())

    @Test
    fun contactsScrollWithBaselineProfile() = contactsScroll(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun coldStartup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = packageName,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = STARTUP_ITERATIONS,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            awaitContactsList()
        }
    }

    private fun contactsScroll(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = packageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.COLD,
            iterations = SCROLL_ITERATIONS,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                awaitContactsList()
            },
        ) {
            scrollContactsList()
        }
    }
}
