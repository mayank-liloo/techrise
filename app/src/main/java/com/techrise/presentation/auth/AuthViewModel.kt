package com.techrise.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techrise.data.remote.LoginResponse
import com.techrise.data.remote.RegisterRequest
import com.techrise.data.remote.LoginRequest
import com.techrise.data.repository.TechRiseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val role: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: TechRiseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _registrationSuccess = MutableStateFlow<String?>(null)
    val registrationSuccess: StateFlow<String?> = _registrationSuccess.asStateFlow()

    init {
        // Automatically route to home if already logged in
        if (repository.isLoggedIn()) {
            _uiState.value = AuthUiState.Success(repository.getRole() ?: "CUSTOMER")
        }
    }

    fun login(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.login(LoginRequest(email, password))
                .onSuccess { response ->
                    _uiState.value = AuthUiState.Success(response.user.role)
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(
                        exception.message ?: "Authentication failed. Please check credentials."
                    )
                }
        }
    }

    fun register(email: String, password: String, name: String, role: String, adminSecret: String? = null, mobile: String? = null) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            repository.register(RegisterRequest(email = email, password = password, name = name, role = role, adminSecret = adminSecret, mobile = mobile))
                .onSuccess { response ->
                    _registrationSuccess.value = response.message
                    _uiState.value = AuthUiState.Idle
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(
                        exception.message ?: "Registration failed. Try using another email."
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }

    fun clearRegistrationStatus() {
        _registrationSuccess.value = null
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState.Idle
    }
}
