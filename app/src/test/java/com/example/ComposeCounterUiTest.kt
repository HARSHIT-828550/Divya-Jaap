package com.example

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ComposeCounterUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `tapping Jaap button increments counter and undo decrements it`() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("jaap_tap_button").assertExists()
            }.isSuccess
        }

        composeRule.onNodeWithTag("jaap_tap_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("today_jaap_value").assertTextContains("1")
            }.isSuccess
        }

        composeRule.onNodeWithTag("jaap_tap_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("today_jaap_value").assertTextContains("2")
            }.isSuccess
        }

        composeRule.onNodeWithTag("undo_tap_button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("today_jaap_value").assertTextContains("1")
            }.isSuccess
        }
    }

    @Test
    fun `navigation across Mantra Guru and Library screens renders cleanly`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_guru").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("guru_play_pause_button").assertExists()
            }.isSuccess
        }

        composeRule.onNodeWithTag("nav_library").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("library_search_input").assertExists()
            }.isSuccess
        }
    }
}
