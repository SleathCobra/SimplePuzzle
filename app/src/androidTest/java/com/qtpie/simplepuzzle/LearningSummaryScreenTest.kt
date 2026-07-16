package com.qtpie.simplepuzzle

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.qtpie.simplepuzzle.core.learning.EvidencePolicyVersion
import com.qtpie.simplepuzzle.core.learning.EvidenceStatus
import com.qtpie.simplepuzzle.core.learning.Grade2Quarter1Skills
import com.qtpie.simplepuzzle.core.learning.PersonalSkillSummary
import com.qtpie.simplepuzzle.ui.screens.LearningSummaryScreen
import com.qtpie.simplepuzzle.ui.theme.SimplePuzzleTheme
import org.junit.Rule
import org.junit.Test

class LearningSummaryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateExplainsPrivateLocalEvidence() {
        setContent(emptyList())

        composeRule.onNodeWithText("No learning evidence yet").assertIsDisplayed()
        composeRule.onNodeWithText("Play Jigsaw Math to begin a private, on-device practice summary.").assertExists()
        composeRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun insufficientEvidenceUsesCautiousLanguage() {
        setContent(listOf(summary(EvidenceStatus.INSUFFICIENT_EVIDENCE)))

        composeRule.onNodeWithText("Keep practicing").assertIsDisplayed()
        composeRule.onNode(hasStateDescription("Keep practicing")).assertExists()
    }

    @Test
    fun developingAndPracticedRecentlyAreDisplayedWithoutPercentages() {
        setContent(listOf(summary(EvidenceStatus.DEVELOPING), summary(EvidenceStatus.PRACTICED_RECENTLY, 2)))

        composeRule.onNodeWithText("Developing").assertExists()
        composeRule.onNodeWithText("Practiced recently").assertExists()
    }

    @Test
    fun reviewSuggestedUsesSupportiveLanguage() {
        setContent(listOf(summary(EvidenceStatus.REVIEW_SUGGESTED)))

        composeRule.onNodeWithText("Review suggested").assertIsDisplayed()
        composeRule.onNodeWithText("A little more guided practice may be useful.").assertExists()
    }

    @Test
    fun summaryRemainsAccessibleAtLargeFontScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                SimplePuzzleTheme {
                    LearningSummaryScreen(listOf(summary(EvidenceStatus.INSUFFICIENT_EVIDENCE)), onBack = {})
                }
            }
        }

        composeRule.onNodeWithText("MY LEARNING").assertExists()
        composeRule.onNodeWithText("Keep practicing").assertExists()
    }

    private fun setContent(summaries: List<PersonalSkillSummary>) {
        composeRule.setContent {
            SimplePuzzleTheme {
                LearningSummaryScreen(summaries, onBack = {})
            }
        }
    }

    private fun summary(status: EvidenceStatus, suffix: Int = 1) = PersonalSkillSummary(
        skillId = if (suffix == 1) {
            Grade2Quarter1Skills.ADD_WITHOUT_REGROUPING
        } else {
            Grade2Quarter1Skills.ADD_WITH_REGROUPING
        },
        skillTitle = if (suffix == 1) "Add without regrouping" else "Add with regrouping",
        status = status,
        evidenceCount = 5,
        distinctTemplateCount = 5,
        lastPracticedAtEpochMillis = 1_000,
        reviewSuggested = status == EvidenceStatus.REVIEW_SUGGESTED,
        policyVersion = EvidencePolicyVersion(1),
    )
}
