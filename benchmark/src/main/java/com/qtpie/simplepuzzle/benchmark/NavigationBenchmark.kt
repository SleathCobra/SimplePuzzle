package com.qtpie.simplepuzzle.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavigationBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun titleToGameplay() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
            iterations = 5,
            setupBlock = {
                pressHome()
                killProcess()
                startActivityAndWait()
            },
    ) {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).openGameplay()
    }

    @Test
    fun firstIncorrectAnswerToLearningSummary() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        setupBlock = {
            pressHome()
            killProcess()
            startActivityAndWait()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).openGameplay()
        },
    ) {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .recordIncorrectAdditionAndOpenLearningSummary()
    }

    @Test
    fun titleToSettings() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
            iterations = 5,
            setupBlock = {
                pressHome()
                killProcess()
                startActivityAndWait()
            },
    ) {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).openSettings()
    }
}
