package com.techrise.presentation.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techrise.data.remote.ComplaintLogResponse
import com.techrise.data.remote.ComplaintResponse
import com.techrise.data.remote.NewsResponse
import com.techrise.data.remote.BannerResponse
import com.techrise.data.repository.TechRiseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ComplaintsUiState {
    object Idle : ComplaintsUiState
    object Loading : ComplaintsUiState
    data class Success(val complaints: List<ComplaintResponse>) : ComplaintsUiState
    data class Error(val message: String) : ComplaintsUiState
}

sealed interface SingleComplaintUiState {
    object Idle : SingleComplaintUiState
    object Loading : SingleComplaintUiState
    data class Success(
        val complaint: ComplaintResponse,
        val logs: List<ComplaintLogResponse>
    ) : SingleComplaintUiState
    data class Error(val message: String) : SingleComplaintUiState
}

sealed interface CreateComplaintUiState {
    object Idle : CreateComplaintUiState
    object Loading : CreateComplaintUiState
    object Success : CreateComplaintUiState
    data class Error(val message: String) : CreateComplaintUiState
}

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: TechRiseRepository
) : ViewModel() {

    private val _complaintsState = MutableStateFlow<ComplaintsUiState>(ComplaintsUiState.Idle)
    val complaintsState: StateFlow<ComplaintsUiState> = _complaintsState.asStateFlow()

    private val _singleComplaintState = MutableStateFlow<SingleComplaintUiState>(SingleComplaintUiState.Idle)
    val singleComplaintState: StateFlow<SingleComplaintUiState> = _singleComplaintState.asStateFlow()

    private val _createComplaintState = MutableStateFlow<CreateComplaintUiState>(CreateComplaintUiState.Idle)
    val createComplaintState: StateFlow<CreateComplaintUiState> = _createComplaintState.asStateFlow()

    private val _newsState = MutableStateFlow<List<NewsResponse>>(emptyList())
    val newsState: StateFlow<List<NewsResponse>> = _newsState.asStateFlow()

    private val _bannersState = MutableStateFlow<List<BannerResponse>>(emptyList())
    val bannersState: StateFlow<List<BannerResponse>> = _bannersState.asStateFlow()

    fun loadComplaints() {
        _complaintsState.value = ComplaintsUiState.Loading
        viewModelScope.launch {
            repository.getComplaints()
                .onSuccess { list ->
                    _complaintsState.value = ComplaintsUiState.Success(list)
                }
                .onFailure { exception ->
                    _complaintsState.value = ComplaintsUiState.Error(
                        exception.message ?: "Failed to fetch complaints."
                    )
                }
        }
    }

    fun loadComplaintDetails(id: String) {
        _singleComplaintState.value = SingleComplaintUiState.Loading
        viewModelScope.launch {
            val complaintResult = repository.getComplaintById(id)
            val logsResult = repository.getComplaintLogs(id)

            if (complaintResult.isSuccess && logsResult.isSuccess) {
                _singleComplaintState.value = SingleComplaintUiState.Success(
                    complaint = complaintResult.getOrThrow(),
                    logs = logsResult.getOrThrow()
                )
            } else {
                val errMsg = complaintResult.exceptionOrNull()?.message 
                    ?: logsResult.exceptionOrNull()?.message 
                    ?: "Failed to load complaint details."
                _singleComplaintState.value = SingleComplaintUiState.Error(errMsg)
            }
        }
    }

    fun createComplaint(title: String, description: String, priority: String, imageBase64: String? = null) {
        _createComplaintState.value = CreateComplaintUiState.Loading
        viewModelScope.launch {
            repository.createComplaint(title, description, priority, imageBase64)
                .onSuccess {
                    _createComplaintState.value = CreateComplaintUiState.Success
                    loadComplaints()
                }
                .onFailure { exception ->
                    _createComplaintState.value = CreateComplaintUiState.Error(
                        exception.message ?: "Failed to submit complaint."
                    )
                }
        }
    }

    fun submitFeedback(id: String, rating: Int, comment: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.submitFeedback(id, rating, comment)
                .onSuccess {
                    loadComplaintDetails(id)
                    loadComplaints()
                    onResult(true, null)
                }
                .onFailure { exception ->
                    onResult(false, exception.message ?: "Unknown error occurred")
                }
        }
    }

    fun loadNewsFeed() {
        viewModelScope.launch {
            repository.getNewsList()
                .onSuccess { list ->
                    _newsState.value = list
                }
                .onFailure {
                    // Handle news feed retrieval failure
                }
        }
    }

    fun loadBanners() {
        viewModelScope.launch {
            repository.getBanners()
                .onSuccess { list ->
                    _bannersState.value = list
                }
                .onFailure {
                    // Fallback is handled in UI
                }
        }
    }

    fun resetCreateState() {
        _createComplaintState.value = CreateComplaintUiState.Idle
    }

    fun getEmail(): String = repository.getEmail() ?: ""
    
    fun logout() {
        repository.logout()
    }
}
