package com.omsharma.iykyk.state

/**
 * Represents various states in an API call or UI interaction, allowing for a consistent way to handle
 * success, failure, and intermediate states across the application.
 *
 * @param T The type of data associated with the state.
 *
 * States:
 * - [Idle]: Indicates that no API call or operation is currently active.
 * - [Loading]: Indicates that an API call or operation is in progress.
 * - [Success]: Indicates that an API call or operation completed successfully, with the associated data.
 * - [Failed]: Indicates that an API call or operation failed, providing an error message.
 */
sealed interface UiState<out T> {

    data object Idle : UiState<Nothing>
    data class Loading(val stage: String? = null) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Failed(val message: String) : UiState<Nothing>
}
