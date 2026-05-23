package com.example.caloriestracker

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.caloriestracker.ui.editor.EditorScreen
import com.example.caloriestracker.ui.main.MainScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(
                    onNavigateToEditor = { entryId -> backStack.add(Editor(entryId)) }
                )
            }
            entry<Editor> { key ->
                EditorScreen(
                    entryId = key.entryId,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
