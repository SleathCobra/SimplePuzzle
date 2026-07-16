package com.qtpie.simplepuzzle.benchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

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

internal fun UiDevice.openSettings() {
    require(wait(Until.hasObject(By.desc("Settings")), UI_TIMEOUT_MILLIS)) {
        "Settings action did not appear."
    }
    findObject(By.desc("Settings")).click()
    require(wait(Until.hasObject(By.text("SETTINGS")), UI_TIMEOUT_MILLIS)) {
        "Settings screen did not appear."
    }
}
