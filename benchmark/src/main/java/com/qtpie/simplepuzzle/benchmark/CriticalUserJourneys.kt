package com.qtpie.simplepuzzle.benchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

internal const val TARGET_PACKAGE = "com.qtpie.simplepuzzle"
private const val UI_TIMEOUT_MILLIS = 10_000L

internal fun UiDevice.openGallery() {
    require(wait(Until.hasObject(By.text("PLAY")), UI_TIMEOUT_MILLIS)) { "Title screen did not appear." }
    findObject(By.text("PLAY")).click()
    require(wait(Until.hasObject(By.text("PUZZLE GALLERY")), UI_TIMEOUT_MILLIS)) {
        "Gallery did not appear."
    }
}

internal fun UiDevice.openGameplay() {
    openGallery()
    require(wait(Until.hasObject(By.desc("Cosmic Journey")), UI_TIMEOUT_MILLIS)) {
        "Cosmic Journey card did not appear."
    }
    findObject(By.desc("Cosmic Journey")).click()
    require(wait(Until.hasObject(By.text("COSMIC JOURNEY")), UI_TIMEOUT_MILLIS)) {
        "Gameplay did not appear."
    }
}

internal fun UiDevice.recordIncorrectAdditionAndOpenLearningSummary() {
    val equationPattern = Pattern.compile("\\d+\\s*\\+\\s*\\d+\\s*=\\s*\\?")
    waitForIdle(UI_TIMEOUT_MILLIS)
    val equation = requireNotNull(wait(Until.findObject(By.text(equationPattern)), UI_TIMEOUT_MILLIS)) {
        "Addition question did not appear."
    }
    val equationText = equation.text
    val operands = requireNotNull(
        Regex("(\\d+)\\s*\\+\\s*(\\d+)\\s*=\\s*\\?").matchEntire(equationText),
    ) { "Unexpected addition question format: $equationText" }
    val answer = operands.groupValues[1].toInt() + operands.groupValues[2].toInt()
    val answerButton = requireNotNull(
        findObjects(By.text(Pattern.compile("\\d+"))).firstOrNull { candidate ->
            candidate.visibleCenter.y > equation.visibleCenter.y && candidate.text.toIntOrNull() != answer
        },
    ) {
        "Incorrect answer button did not appear."
    }

    executeShellCommand("input tap ${answerButton.visibleCenter.x} ${answerButton.visibleCenter.y}")
    waitForIdle(UI_TIMEOUT_MILLIS)
    pressBack()
    require(wait(Until.hasObject(By.text("PUZZLE GALLERY")), UI_TIMEOUT_MILLIS)) {
        "Gameplay did not tear down to the gallery."
    }
    pressBack()
    require(wait(Until.hasObject(By.text("PLAY")), UI_TIMEOUT_MILLIS)) {
        "Gallery did not return to the title screen."
    }
    require(wait(Until.hasObject(By.desc("My learning")), UI_TIMEOUT_MILLIS)) {
        "My learning action did not appear."
    }
    findObject(By.desc("My learning")).click()
    require(wait(Until.hasObject(By.text("MY LEARNING")), UI_TIMEOUT_MILLIS)) {
        "Learning summary did not appear."
    }
    require(
        wait(
            Until.hasObject(By.text(Pattern.compile("Recent evidence: [1-9]\\d*"))),
            UI_TIMEOUT_MILLIS,
        ),
    ) {
        "Recorded attempt did not appear in the local learning summary."
    }
}

internal fun UiDevice.openSettings() {
    require(wait(Until.hasObject(By.desc("Settings")), UI_TIMEOUT_MILLIS)) {
        "Settings action did not appear."
    }
    findObject(By.desc("Settings")).click()
    require(wait(Until.hasObject(By.text("SETTINGS")), UI_TIMEOUT_MILLIS)) {
        "Settings screen did not appear."
    }
}
