package com.techrise.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techrise.data.remote.ComplaintLogResponse
import com.techrise.data.remote.ComplaintResponse
import com.techrise.data.remote.EmployeeResponse
import com.techrise.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminComplaintDetailsScreen(
    complaintId: String,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val selectedComplaint by viewModel.selectedComplaint.collectAsState()
    val logs by viewModel.complaintLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val employees by viewModel.employees.collectAsState()

    var statusInput by remember { mutableStateOf("IN_PROGRESS") }
    var priorityInput by remember { mutableStateOf("MEDIUM") }
    var assignedAdminInput by remember { mutableStateOf<String?>(null) }
    var commentInput by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(complaintId) {
        viewModel.loadComplaintDetails(complaintId)
    }

    LaunchedEffect(selectedComplaint) {
        selectedComplaint?.let {
            statusInput = it.status.uppercase()
            priorityInput = it.priority.uppercase()
            assignedAdminInput = it.assignedAdminId
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Update Successful") },
            text = { Text("The complaint status and action log have been successfully updated.") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complaint Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading && selectedComplaint == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null && selectedComplaint == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                    Button(onClick = onBack) { Text("Back") }
                }
            } else {
                selectedComplaint?.let { complaint ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Complaint overview card
                        item {
                            AdminComplaintOverviewCard(complaint = complaint)
                        }

                        // 2. Action Center Card (Only shown if status is not resolved, or allows edits)
                        item {
                            AdminActionCenterCard(
                                complaint = complaint,
                                statusInput = statusInput,
                                onStatusChange = { statusInput = it },
                                priorityInput = priorityInput,
                                onPriorityChange = { priorityInput = it },
                                employees = employees,
                                assignedAdminInput = assignedAdminInput,
                                onAssignedAdminChange = { assignedAdminInput = it },
                                commentInput = commentInput,
                                onCommentChange = { commentInput = it },
                                isLoading = isLoading,
                                onUpdate = {
                                    viewModel.updateComplaintStatus(
                                        id = complaint.id,
                                        newStatus = statusInput,
                                        comment = commentInput,
                                        priority = priorityInput,
                                        assignedAdminId = assignedAdminInput
                                    ) {
                                        commentInput = ""
                                        showSuccessDialog = true
                                    }
                                }
                            )
                        }

                        // 3. Customer Rating Card (If resolved and rating exists)
                        if (complaint.status.uppercase() == "RESOLVED" && complaint.rating != null) {
                            item {
                                CustomerFeedbackCard(
                                    rating = complaint.rating,
                                    comment = complaint.feedbackComment ?: "No comment provided."
                                )
                            }
                        }

                        // 4. Visual Timeline Audit Logs
                        item {
                            Text(
                                text = "Audit Trail Timeline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        if (logs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No audit log events recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            itemsIndexed(logs) { index, log ->
                                VisualTimelineRow(log = log, isLast = index == logs.lastIndex)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminComplaintOverviewCard(complaint: ComplaintResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Complaint #${complaint.id.take(8).uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                ComplaintStatusBadge(status = complaint.status)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = complaint.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = complaint.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Client", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val clientText = if (!complaint.customerEmail.isNullOrBlank()) complaint.customerEmail else complaint.customerId
                    Text(clientText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Priority", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        complaint.priority.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (complaint.priority.uppercase()) {
                            "HIGH" -> PriorityHigh
                            "MEDIUM" -> PriorityMedium
                            else -> PriorityLow
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Assigned Staff", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val staffText = if (!complaint.assignedAdminEmail.isNullOrBlank()) complaint.assignedAdminEmail else (complaint.assignedAdminId ?: "Unassigned")
                    Text(staffText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Raised Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val dateStr = complaint.createdAt?.let {
                        val date = Date(it._seconds * 1000)
                        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
                    } ?: "N/A"
                    Text(dateStr, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun AdminActionCenterCard(
    complaint: ComplaintResponse,
    statusInput: String,
    onStatusChange: (String) -> Unit,
    priorityInput: String,
    onPriorityChange: (String) -> Unit,
    employees: List<EmployeeResponse>,
    assignedAdminInput: String?,
    onAssignedAdminChange: (String?) -> Unit,
    commentInput: String,
    onCommentChange: (String) -> Unit,
    isLoading: Boolean,
    onUpdate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Staff Action Center",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text("Set Complaint Status:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = statusInput == "PENDING",
                    onClick = { onStatusChange("PENDING") },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = statusInput == "IN_PROGRESS",
                    onClick = { onStatusChange("IN_PROGRESS") },
                    label = { Text("Active") }
                )
                FilterChip(
                    selected = statusInput == "RESOLVED",
                    onClick = { onStatusChange("RESOLVED") },
                    label = { Text("Resolved") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Set Complaint Priority:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = priorityInput == "LOW",
                    onClick = { onPriorityChange("LOW") },
                    label = { Text("Low") }
                )
                FilterChip(
                    selected = priorityInput == "MEDIUM",
                    onClick = { onPriorityChange("MEDIUM") },
                    label = { Text("Medium") }
                )
                FilterChip(
                    selected = priorityInput == "HIGH",
                    onClick = { onPriorityChange("HIGH") },
                    label = { Text("High") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Manual Employee Assignment Dropdown
            var showAssignDropdown by remember { mutableStateOf(false) }
            val selectedEmployeeEmail = employees.firstOrNull { it.id == assignedAdminInput }?.email ?: "Unassigned (Claims Automatically)"

            Text("Assign Staff Member:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showAssignDropdown = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (assignedAdminInput != null) "$selectedEmployeeEmail ($assignedAdminInput)" else selectedEmployeeEmail,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = showAssignDropdown,
                    onDismissRequest = { showAssignDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Unassigned / Auto-Claim") },
                        onClick = {
                            onAssignedAdminChange(null)
                            showAssignDropdown = false
                        }
                    )
                    for (emp in employees) {
                        DropdownMenuItem(
                            text = { Text("${emp.email} (${emp.id})") },
                            onClick = {
                                onAssignedAdminChange(emp.id)
                                showAssignDropdown = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = commentInput,
                onValueChange = onCommentChange,
                label = { Text("Action Log / Resolution Comment") },
                placeholder = { Text("e.g. Contacted client and successfully restored login credentials.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUpdate,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Update Complaint & Log Event")
                }
            }
        }
    }
}

@Composable
fun CustomerFeedbackCard(
    rating: Int,
    comment: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), // Soft green card
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Customer Satisfaction Feedback",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = if (index < rating) Color(0xFFFFB300) else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($rating / 5 Stars)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"$comment\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
fun VisualTimelineRow(log: ComplaintLogResponse, isLast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when (log.newStatus.uppercase()) {
                            "PENDING" -> StatusPending
                            "IN_PROGRESS" -> StatusActive
                            "RESOLVED" -> StatusResolved
                            else -> Color.Gray
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (log.newStatus.uppercase() == "RESOLVED") {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            val dateStr = log.createdAt.let {
                val date = Date(it._seconds * 1000)
                SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
            }
            val statusText = if (log.oldStatus == "NONE") {
                "Complaint Registered"
            } else {
                "${log.oldStatus} ➔ ${log.newStatus}"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "By User ID: ${log.actionBy}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = log.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }
}
