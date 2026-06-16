package com.techrise.data.repository

import com.techrise.data.local.SessionManager
import com.techrise.data.remote.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TechRiseRepository @Inject constructor(
    private val apiService: TechRiseApiService,
    private val sessionManager: SessionManager
) {

    // --- Authentication ---

    suspend fun login(request: LoginRequest): Result<LoginResponse> = runCatching {
        val response = apiService.login(request)
        sessionManager.saveSession(
            token = response.token,
            userId = response.user.id,
            email = response.user.email,
            role = response.user.role
        )
        response
    }

    suspend fun register(request: RegisterRequest): Result<RegisterResponse> = runCatching {
        apiService.register(request)
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
    fun getRole(): String? = sessionManager.getRole()
    fun getEmail(): String? = sessionManager.getEmail()
    fun getUserId(): String? = sessionManager.getUserId()

    // Helper to format Bearer Token header
    private fun getAuthHeader(): String {
        val token = sessionManager.getToken() ?: ""
        return "Bearer $token"
    }

    // --- Complaints (Tickets) ---

    suspend fun createComplaint(title: String, description: String, priority: String): Result<CreateComplaintResponse> = runCatching {
        apiService.createComplaint(
            token = getAuthHeader(),
            request = CreateComplaintRequest(title, description, priority)
        )
    }

    suspend fun getComplaints(): Result<List<ComplaintResponse>> = runCatching {
        apiService.getComplaints(getAuthHeader())
    }

    suspend fun getComplaintById(id: String): Result<ComplaintResponse> = runCatching {
        apiService.getComplaintById(getAuthHeader(), id)
    }

    suspend fun getComplaintLogs(id: String): Result<List<ComplaintLogResponse>> = runCatching {
        apiService.getComplaintLogs(getAuthHeader(), id)
    }

    suspend fun submitFeedback(id: String, rating: Int, comment: String): Result<FeedbackResponse> = runCatching {
        apiService.submitFeedback(
            token = getAuthHeader(),
            id = id,
            request = FeedbackRequest(rating, comment)
        )
    }

    // --- News / Bulletin Board ---

    suspend fun getNewsList(): Result<List<NewsResponse>> = runCatching {
        apiService.getNewsList(getAuthHeader())
    }
}
