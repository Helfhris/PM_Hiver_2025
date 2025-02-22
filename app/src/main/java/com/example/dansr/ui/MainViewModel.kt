package com.example.dansr.ui

import androidx.lifecycle.ViewModel
import com.example.dansr.DansRScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class UIState(val selectedScreen: DansRScreen = DansRScreen.Start)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UIState())
    val uiState: StateFlow<UIState> = _uiState

    fun updateSelectedScreen(screen: DansRScreen) {
        _uiState.update { it.copy(selectedScreen = screen) }
    }
}
