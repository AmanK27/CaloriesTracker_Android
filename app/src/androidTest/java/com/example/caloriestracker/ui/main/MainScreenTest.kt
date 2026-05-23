package com.example.caloriestracker.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.caloriestracker.ui.main.MainScreen] components. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun emptyState_displaysEmptyMessage() {
    composeTestRule.setContent {
      EmptyState(hasQuery = false)
    }
    composeTestRule.onNodeWithText("Your journal is empty.").assertExists()
    composeTestRule.onNodeWithText("Tap the + button to write your first entry today!").assertExists()
  }

  @Test
  fun emptyState_displaysNoResultsMessage() {
    composeTestRule.setContent {
      EmptyState(hasQuery = true)
    }
    composeTestRule.onNodeWithText("No matching entries found.").assertExists()
    composeTestRule.onNodeWithText("Try adjusting your search keywords.").assertExists()
  }
}
