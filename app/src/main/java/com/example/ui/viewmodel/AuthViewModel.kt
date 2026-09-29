package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.AppRepository
import com.example.data.security.SecurityResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val selectedRole: UserRole = UserRole.ADMIN,
    val usernameInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    fun onRoleSelected(role: UserRole) {
        _uiState.value = _uiState.value.copy(
            selectedRole = role,
            errorMessage = null
        )
    }

    fun onUsernameChanged(input: String) {
        _uiState.value = _uiState.value.copy(usernameInput = input, errorMessage = null)
    }

    fun onPasswordChanged(input: String) {
        _uiState.value = _uiState.value.copy(passwordInput = input, errorMessage = null)
    }

    fun login(onSuccess: (UserEntity) -> Unit) {
        val state = _uiState.value
        if (state.usernameInput.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Vul uw gebruikersnaam in.")
            return
        }
        if (state.passwordInput.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Vul uw wachtwoord in.")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = repository.login(state.usernameInput, state.passwordInput, state.selectedRole)) {
                is SecurityResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        passwordInput = ""
                    )
                    onSuccess(result.data)
                }
                is SecurityResult.Denied -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.reason
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
