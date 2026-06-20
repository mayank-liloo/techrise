package com.techrise.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techrise.data.remote.ComplaintLogResponse
import com.techrise.data.remote.ComplaintResponse
import com.techrise.data.remote.EmployeeResponse
import com.techrise.data.remote.NewsResponse
import com.techrise.data.remote.FeedbackResponse
import com.techrise.data.repository.TechRiseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: TechRiseRepository
) : ViewModel() {

    // --- State: Employees list ---
    private val _employees = MutableStateFlow<List<EmployeeResponse>>(emptyList())
    val employees: StateFlow<List<EmployeeResponse>> = _employees.asStateFlow()

    // --- State: Complaints list ---
    private val _complaints = MutableStateFlow<List<ComplaintResponse>>(emptyList())
    val complaints: StateFlow<List<ComplaintResponse>> = _complaints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- State: Selected Complaint Details ---
    private val _selectedComplaint = MutableStateFlow<ComplaintResponse?>(null)
    val selectedComplaint: StateFlow<ComplaintResponse?> = _selectedComplaint.asStateFlow()

    private val _complaintLogs = MutableStateFlow<List<ComplaintLogResponse>>(emptyList())
    val complaintLogs: StateFlow<List<ComplaintLogResponse>> = _complaintLogs.asStateFlow()

    // --- State: News publishing ---
    private val _newsPublishedSuccess = MutableStateFlow(false)
    val newsPublishedSuccess: StateFlow<Boolean> = _newsPublishedSuccess.asStateFlow()

    private val _newsList = MutableStateFlow<List<NewsResponse>>(emptyList())
    val newsList: StateFlow<List<NewsResponse>> = _newsList.asStateFlow()

    init {
        loadAllComplaints()
        loadEmployees()
        loadNewsList()
    }

    fun loadAllComplaints() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getComplaints()
                .onSuccess { list ->
                    _complaints.value = list
                }
                .onFailure { err ->
                    _error.value = err.message ?: "Failed to fetch complaints"
                }
            _isLoading.value = false
        }
    }

    fun loadComplaintDetails(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Get details
            repository.getComplaintById(id)
                .onSuccess { details ->
                    _selectedComplaint.value = details
                }
                .onFailure { err ->
                    _error.value = err.message ?: "Failed to fetch complaint details"
                }

            // Get timeline logs
            repository.getComplaintLogs(id)
                .onSuccess { logs ->
                    _complaintLogs.value = logs
                }
                .onFailure { err ->
                    _complaintLogs.value = emptyList()
                }

            _isLoading.value = false
        }
    }

    fun loadEmployees() {
        viewModelScope.launch {
            repository.getEmployees()
                .onSuccess { list ->
                    _employees.value = list
                }
                .onFailure { err ->
                    _error.value = err.message ?: "Failed to fetch employees list"
                }
        }
    }

    fun updateComplaintStatus(
        id: String,
        newStatus: String,
        comment: String,
        priority: String? = null,
        assignedAdminId: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.updateComplaintStatus(id, newStatus, comment, priority, assignedAdminId)
                .onSuccess {
                    loadComplaintDetails(id)
                    loadAllComplaints()
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.message ?: "Failed to update complaint status"
                }
            _isLoading.value = false
        }
    }

    fun loadNewsList() {
        viewModelScope.launch {
            repository.getNewsList()
                .onSuccess { list ->
                    _newsList.value = list
                }
                .onFailure { err ->
                    _error.value = err.message ?: "Failed to fetch news bulletins"
                }
        }
    }

    fun publishNews(title: String, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _newsPublishedSuccess.value = false
            repository.createNews(title, content)
                .onSuccess {
                    _newsPublishedSuccess.value = true
                    loadNewsList()
                    onSuccess()
                }
                .onFailure { err ->
                    _error.value = err.message ?: "Failed to publish news bulletin"
                }
            _isLoading.value = false
        }
    }

    fun deleteNews(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.deleteNews(id)
                .onSuccess {
                    loadNewsList()
                }
                .onFailure { err ->
                    _error.value = err.message ?: "Failed to delete news bulletin"
                }
            _isLoading.value = false
        }
    }

    fun resetNewsPublishSuccess() {
        _newsPublishedSuccess.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun logout() {
        repository.logout()
    }

    // Helper: computes whether a complaint has been unresolved for >7 days
    fun isComplaintEscalated(complaint: ComplaintResponse): Boolean {
        if (complaint.status == "RESOLVED") return false
        val seconds = complaint.createdAt?._seconds ?: return false
        val sevenDaysInSeconds = 7 * 24 * 60 * 60
        val currentSeconds = System.currentTimeMillis() / 1000
        return (currentSeconds - seconds) > sevenDaysInSeconds
    }
}
