package com.example.presentation.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.MantraCategory
import com.example.database.MantraWithFavorite
import com.example.presentation.theme.CoralMistake
import com.example.presentation.theme.IvoryText
import com.example.presentation.theme.MutedText
import com.example.presentation.theme.NavyBorder
import com.example.presentation.theme.NavySanctum
import com.example.presentation.theme.NavyVoid
import com.example.presentation.theme.SacredGold
import com.example.presentation.theme.SacredGoldBright
import com.example.presentation.theme.SandText

@Composable
fun MantraLibraryScreen(
    viewModel: MantraLibraryViewModel,
    onStartJaap: () -> Unit,
    onOpenGuru: (String, String) -> Unit,
    onOpenPractice: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyVoid)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = "Mantra Library",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SacredGoldBright
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search mantra, deity, or meaning...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips + Favorites Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.favoritesOnly,
                        onClick = { viewModel.toggleFavoritesOnly() },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Favorites") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SacredGold,
                            selectedLabelColor = NavyVoid,
                            containerColor = NavySanctum,
                            labelColor = SandText
                        )
                    )
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SacredGold,
                            selectedLabelColor = NavyVoid,
                            containerColor = NavySanctum,
                            labelColor = SandText
                        )
                    )
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.selectedCategoryId == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SacredGold,
                                selectedLabelColor = NavyVoid,
                                containerColor = NavySanctum,
                                labelColor = SandText
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.mantras, key = { it.id }) { mantra ->
                        MantraLibraryCard(
                            mantra = mantra,
                            categoryName = state.categories.firstOrNull { it.id == mantra.categoryId }?.name ?: "Mantra",
                            onToggleFavorite = { viewModel.toggleFavorite(mantra.id) },
                            onStartJaap = { viewModel.startJaapWithMantra(mantra, onStartJaap) },
                            onPlayGuru = { onOpenGuru(mantra.title, mantra.sanskritText) },
                            onPractice = { onOpenPractice(mantra.id) },
                            onEdit = { viewModel.openEditMantraDialog(mantra) },
                            onDelete = { viewModel.deleteCustomMantra(mantra.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.openAddMantraDialog() },
            containerColor = SacredGold,
            contentColor = NavyVoid,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_mantra_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add custom mantra")
        }
    }

    if (state.showEditorDialog) {
        MantraEditorDialog(
            categories = state.categories,
            existing = state.editingMantra,
            onDismiss = { viewModel.closeEditorDialog() },
            onSave = { catId, title, sanskrit, translit, meaning, target ->
                viewModel.saveMantra(catId, title, sanskrit, translit, meaning, target)
            }
        )
    }
}

@Composable
private fun MantraLibraryCard(
    mantra: MantraWithFavorite,
    categoryName: String,
    onToggleFavorite: () -> Unit,
    onStartJaap: () -> Unit,
    onPlayGuru: () -> Unit,
    onPractice: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySanctum),
        border = BorderStroke(1.dp, NavyBorder),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = SacredGold
                    )
                    Text(
                        text = mantra.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = IvoryText
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (mantra.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (mantra.isFavorite) "Remove favorite" else "Add to favorites",
                        tint = if (mantra.isFavorite) SacredGoldBright else SandText
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit mantra",
                        tint = SandText
                    )
                }
                if (mantra.isCustom) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete custom mantra",
                            tint = CoralMistake
                        )
                    }
                }
            }

            Text(
                text = mantra.sanskritText,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp),
                color = SacredGoldBright
            )
            if (mantra.transliteration.isNotBlank()) {
                Text(
                    text = mantra.transliteration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedText
                )
            }
            if (mantra.hindiMeaning.isNotBlank()) {
                Text(
                    text = "अर्थ: ${mantra.hindiMeaning}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SandText
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartJaap,
                    colors = ButtonDefaults.buttonColors(containerColor = SacredGold, contentColor = NavyVoid),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Jaap")
                }
                OutlinedButton(
                    onClick = onPlayGuru,
                    border = BorderStroke(1.dp, SacredGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SacredGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Listen", color = SacredGoldBright)
                }
                IconButton(
                    onClick = onPractice,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Practice pronunciation",
                        tint = SacredGold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MantraEditorDialog(
    categories: List<MantraCategory>,
    existing: MantraWithFavorite?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Int) -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(
            categories.firstOrNull { it.id == existing?.categoryId }
                ?: categories.lastOrNull()
                ?: MantraCategory("cat_custom", "Custom Mantra", "व्यक्तिगत मंत्र", "", 9)
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var sanskrit by remember { mutableStateOf(existing?.sanskritText ?: "") }
    var translit by remember { mutableStateOf(existing?.transliteration ?: "") }
    var meaning by remember { mutableStateOf(existing?.hindiMeaning ?: "") }
    var target by remember { mutableStateOf((existing?.defaultTargetCount ?: 108).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NavySanctum,
        title = {
            Text(
                text = if (existing == null) "Add Custom Mantra" else "Edit Mantra",
                color = IvoryText
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Mantra Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sanskrit,
                    onValueChange = { sanskrit = it },
                    label = { Text("Sanskrit / Hindi Text") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = translit,
                    onValueChange = { translit = it },
                    label = { Text("Transliteration (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text("Hindi Meaning") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Default Mala Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && sanskrit.isNotBlank(),
                onClick = {
                    onSave(
                        selectedCategory.id,
                        title,
                        sanskrit,
                        translit,
                        meaning,
                        target.toIntOrNull()?.coerceIn(1, 10008) ?: 108
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SacredGold, contentColor = NavyVoid)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SandText)
            }
        }
    )
}
