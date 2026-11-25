package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun advanceFrames(frames: Int, frameMillis: Long = 16L) {
        repeat(frames) {
            composeTestRule.mainClock.advanceTimeBy(frameMillis)
        }
    }

    @Test
    fun `home play button immediately launches combat screen`() {
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("home_play_button").assertIsDisplayed().performClick()
        advanceFrames(10)

        composeTestRule.onNodeWithTag("player_health_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cpu_health_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ctrl_punch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("combat_pause_button").assertIsDisplayed()

        // Advance past INTRO into FIGHTING and tap punch
        advanceFrames(75)
        composeTestRule.onNodeWithTag("ctrl_punch").performTouchInput { down(center); up() }
        advanceFrames(15)
    }

    @Test
    fun `campaign flow from home to combat screen works`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mode_campaign_card").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("level_card_1").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("start_fight_button").assertIsDisplayed().performClick()
        advanceFrames(10)

        composeTestRule.onNodeWithTag("player_health_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cpu_health_bar").assertIsDisplayed()
    }

    @Test
    fun `versus flow from home to combat pause and resume works`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mode_versus_card").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("start_fight_button").assertIsDisplayed().performClick()
        advanceFrames(10)

        composeTestRule.onNodeWithTag("player_health_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("combat_pause_button").performClick()
        advanceFrames(5)

        composeTestRule.onNodeWithTag("pause_exit_button").assertIsDisplayed().performClick()
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.onNodeWithTag("home_play_button").performClick()
        advanceFrames(10)

        composeTestRule.onNodeWithTag("player_health_bar").assertIsDisplayed()
    }
}
