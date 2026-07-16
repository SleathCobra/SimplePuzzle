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
    fun titleToModeSelection() = benchmarkRule.measureRepeated(
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
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).openModeSelection()
    }

    @Test
    fun timedModeRapidAnswers() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 3,
        setupBlock = {
            pressHome()
            killProcess()
            startActivityAndWait()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).openGameplay("Combo Rush")
        },
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        repeat(10) { device.answerCurrentQuestion() }
    }

    @Test
    fun puzzleDecayRevealAndRemoval() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 3,
        setupBlock = {
            pressHome()
            killProcess()
            startActivityAndWait()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).openGameplay("Puzzle Decay")
        },
    ) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.answerCurrentQuestion(correctly = true)
        device.answerCurrentQuestion(correctly = false)
    }

    @Test
    fun backgroundResumeTimedMode() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 3,
        setupBlock = {
            pressHome()
            killProcess()
            startActivityAndWait()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).openGameplay("Time Attack")
        },
    ) {
        pressHome()
        startActivityAndWait()
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
