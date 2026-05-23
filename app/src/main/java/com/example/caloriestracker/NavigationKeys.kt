package com.example.caloriestracker

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data class Editor(val entryId: String? = null) : NavKey
