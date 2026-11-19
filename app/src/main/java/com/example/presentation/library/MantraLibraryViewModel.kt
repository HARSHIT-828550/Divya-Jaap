package com.example.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.MantraCategory
import com.example.database.MantraWithFavorite
import com.example.domain.JaapRepository
import com.example.domain.MantraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MantraLibraryUiState(
    val categories: List<MantraCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val favoritesOnly: Boolean = false,
    val searchQuery: String = "",
    val mantras: List<MantraWithFavorite> = emptyList(),
    val editingMantra: MantraWithFavorite? = null,
    val showEditorDialog: Boolean = false
)

class MantraLibraryViewModel(
    private val mantraRepository: MantraRepository,
    private val jaapRepository: JaapRepository
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val favoritesOnly = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val editingMantra = MutableStateFlow<MantraWithFavorite?>(null)
    private val showEditorDialog = MutableStateFlow(false)

    val uiState: StateFlow<MantraLibraryUiState> = combine(
        mantraRepository.observeCategories(),
        mantraRepository.observeMantrasWithFavorites(),
        selectedCategoryId,
        favoritesOnly,
        searchQuery,
        editingMantra,
        showEditorDialog
    ) { values ->
        val categories = values[0] as List<MantraCategory>
        val allMantras = values[1] as List<MantraWithFavorite>
        val catId = values[2] as String?
        val favOnly = values[3] as Boolean
        val query = (values[4] as String).trim()
        val editing = values[5] as MantraWithFavorite?
        val showDlg = values[6] as Boolean

        val filtered = allMantras.filter { m ->
            val matchCat = catId == null || m.categoryId == catId
            val matchFav = !favOnly || m.isFavorite
            val matchQuery = query.isEmpty() ||
                m.title.contains(query, ignoreCase = true) ||
                m.sanskritText.contains(query, ignoreCase = true) ||
                m.transliteration.contains(query, ignoreCase = true) ||
                m.hindiMeaning.contains(query, ignoreCase = true)
            matchCat && matchFav && matchQuery
        }

        MantraLibraryUiState(
            categories = categories,
            selectedCategoryId = catId,
            favoritesOnly = favOnly,
            searchQuery = values[4] as String,
            mantras = filtered,
            editingMantra = editing,
            showEditorDialog = showDlg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MantraLibraryUiState()
    )

    fun selectCategory(categoryId: String?) {
        selectedCategoryId.value = categoryId
    }

    fun toggleFavoritesOnly() {
        favoritesOnly.value = !favoritesOnly.value
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun toggleFavorite(mantraId: String) {
        viewModelScope.launch {
            mantraRepository.toggleFavorite(mantraId)
        }
    }

    fun startJaapWithMantra(mantra: MantraWithFavorite, onDone: () -> Unit) {
        viewModelScope.launch {
            jaapRepository.selectActiveMantra(mantra.id, mantra.defaultTargetCount)
            onDone()
        }
    }

    fun openAddMantraDialog() {
        editingMantra.value = null
        showEditorDialog.value = true
    }

    fun openEditMantraDialog(mantra: MantraWithFavorite) {
        editingMantra.value = mantra
        showEditorDialog.value = true
    }

    fun closeEditorDialog() {
        showEditorDialog.value = false
        editingMantra.value = null
    }

    fun saveMantra(
        categoryId: String,
        title: String,
        sanskritText: String,
        transliteration: String,
        hindiMeaning: String,
        defaultTargetCount: Int
    ) {
        val current = editingMantra.value
        showEditorDialog.value = false
        editingMantra.value = null
        viewModelScope.launch {
            mantraRepository.saveCustomMantra(
                id = current?.id,
                categoryId = categoryId,
                title = title,
                sanskritText = sanskritText,
                transliteration = transliteration,
                hindiMeaning = hindiMeaning,
                defaultTargetCount = defaultTargetCount
            )
        }
    }

    fun deleteCustomMantra(id: String) {
        viewModelScope.launch {
            mantraRepository.deleteCustomMantra(id)
        }
    }
}
