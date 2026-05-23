package com.example.caloriestracker.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.caloriestracker.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    entryId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val entry by viewModel.entryState.collectAsState()

    // Trigger load when entryId changes
    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    // Save changes when navigating back
    val onBackWithSave = {
        viewModel.saveChangesImmediately()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write Entry") },
                navigationIcon = {
                    IconButton(onClick = onBackWithSave) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onBackWithSave) {
                        Icon(Icons.Default.Done, contentDescription = "Save and Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        entry?.let { currentEntry ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title Field
                TextField(
                    value = currentEntry.title ?: "",
                    onValueChange = { viewModel.updateTitle(it) },
                    placeholder = { Text("Title (Optional)", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mood Selector
                Text(
                    text = "How are you feeling?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )

                MoodSelectorRow(
                    selectedMood = currentEntry.mood,
                    onMoodSelected = { viewModel.updateMood(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tags row and input
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
                
                TagEditor(
                    tags = currentEntry.tags,
                    onTagAdded = { viewModel.addTag(it) },
                    onTagRemoved = { viewModel.removeTag(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Body Editor Field
                OutlinedTextField(
                    value = currentEntry.content,
                    onValueChange = { viewModel.updateContent(it) },
                    placeholder = { Text("Write your thoughts here...") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    minLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                )
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun MoodSelectorRow(
    selectedMood: String?,
    onMoodSelected: (String?) -> Unit
) {
    val moods = listOf(
        Pair("happy", "😊"),
        Pair("sad", "😢"),
        Pair("calm", "🧘"),
        Pair("anxious", "😰"),
        Pair("energetic", "⚡")
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
    ) {
        moods.forEach { (moodName, emoji) ->
            val isSelected = selectedMood?.lowercase() == moodName
            val color = when (moodName) {
                "happy" -> MoodHappy
                "sad" -> MoodSad
                "calm" -> MoodCalm
                "anxious" -> MoodAnxious
                "energetic" -> MoodEnergetic
                else -> Color.Gray
            }

            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) color.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMoodSelected(if (isSelected) null else moodName) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = emoji, fontSize = 14.sp)
                    Text(
                        text = moodName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun TagEditor(
    tags: List<String>,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit
) {
    var tagInput by remember { mutableStateFlowOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                placeholder = { Text("Add tag...") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Button(
                onClick = {
                    if (tagInput.trim().isNotEmpty()) {
                        onTagAdded(tagInput.trim())
                        tagInput = ""
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Tag")
            }
        }

        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onTagRemoved(tag) },
                        label = { Text(text = "#$tag") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove tag",
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

// Helper Compose state holder utility
private fun <T> mutableStateFlowOf(value: T): MutableState<T> = mutableStateOf(value)
