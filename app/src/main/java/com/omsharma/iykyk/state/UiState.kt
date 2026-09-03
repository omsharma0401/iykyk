package com.omsharma.iykyk.state

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data class Loading(val stage: String? = null, val progress: Float? = null) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Failed(val message: String) : UiState<Nothing>
}
