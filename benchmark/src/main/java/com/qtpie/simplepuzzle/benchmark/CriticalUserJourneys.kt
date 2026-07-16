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

internal fun UiDevice.openModeSelection() {
    openGallery()
    require(wait(Until.hasObject(By.desc("Cosmic Journey")), UI_TIMEOUT_MILLIS)) {
        "Cosmic Journey card did not appear."
    }
    findObject(By.desc("Cosmic Journey")).click()
    require(wait(Until.hasObject(By.text("CHOOSE A MODE")), UI_TIMEOUT_MILLIS)) {
        "Mode selection did not appear."
    }
}

internal fun UiDevice.openGameplay(modeName: String = "Classic") {
    openModeSelection()
    require(wait(Until.hasObject(By.text(modeName)), UI_TIMEOUT_MILLIS)) {
        "$modeName mode did not appear."
    }
    findObject(By.text(modeName)).click()
    require(wait(Until.hasObject(By.text("SCORE")), UI_TIMEOUT_MILLIS)) {
        "Gameplay did not appear."
    }
}

internal fun UiDevice.answerCurrentQuestion(correctly: Boolean = true) {
    val question = requireNotNull(
        wait(Until.findObject(By.desc(Pattern.compile("Question: \\d+ [+-] \\d+"))), UI_TIMEOUT_MILLIS),
    ) { "Question did not appear." }
    val match = requireNotNull(
        Regex("Question: (\\d+) ([+-]) (\\d+)").find(question.contentDescription),
    )
    val left = match.groupValues[1].toInt()
    val right = match.groupValues[3].toInt()
    val answer = if (match.groupValues[2] == "+") left + right else left - right
    val target = if (correctly) {
        requireNotNull(findObject(By.desc("Answer $answer")))
    } else {
        findObjects(By.desc(Pattern.compile("Answer -?\\d+")))
            .first { it.contentDescription != "Answer $answer" }
    }
    target.click()
    if (correctly) {
        wait(
            Until.gone(By.desc(question.contentDescription)),
            UI_TIMEOUT_MILLIS,
        )
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
