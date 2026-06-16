package com.techrise.presentation.complaints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techrise.data.remote.ComplaintLogResponse
import com.techrise.data.remote.ComplaintResponse
import com.techrise.data.remote.NewsResponse
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

sealed interface CreateTicketUiState {
    object Idle : CreateTicketUiState
    object Loading : CreateTicketUiState
    object Success : CreateTicketUiState
    data class Error(val message: String) : CreateTicketUiState
}

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: TechRiseRepository
) : ViewModel() {

    private val _complaintsState = MutableStateFlow<ComplaintsUiState>(ComplaintsUiState.Idle)
    val complaintsState: StateFlow<ComplaintsUiState> = _complaintsState.asStateFlow()

    private val _singleComplaintState = MutableStateFlow<SingleComplaintUiState>(SingleComplaintUiState.Idle)
    val singleComplaintState: StateFlow<SingleComplaintUiState> = _singleComplaintState.asStateFlow()

    private val _createTicketState = MutableStateFlow<CreateTicketUiState>(CreateTicketUiState.Idle)
    val createTicketState: StateFlow<CreateTicketUiState> = _createTicketState.asStateFlow()

    private val _newsState = MutableStateFlow<List<NewsResponse>>(emptyList())
    val newsState: StateFlow<List<NewsResponse>> = _newsState.asStateFlow()

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

    fun createComplaint(title: String, description: String, priority: String) {
        _createTicketState.value = CreateTicketUiState.Loading
        viewModelScope.launch {
            repository.createComplaint(title, description, priority)
                .onSuccess {
                    _createTicketState.value = CreateTicketUiState.Success
                    loadComplaints()
                }
                .onFailure { exception ->
                    _createTicketState.value = CreateTicketUiState.Error(
                        exception.message ?: "Failed to submit ticket."
                    )
                }
        }
    }

    fun submitFeedback(id: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.submitFeedback(id, rating, comment)
                .onSuccess {
                    loadComplaintDetails(id)
                }
                .onFailure {
                    // Handle feedback submission error
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

    fun resetCreateState() {
        _createTicketState.value = CreateTicketUiState.Idle
    }

    fun getEmail(): String = repository.getEmail() ?: ""
    
    fun logout() {
        repository.logout()
    }
}
