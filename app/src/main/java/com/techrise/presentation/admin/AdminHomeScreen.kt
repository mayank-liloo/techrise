package com.techrise.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techrise.data.remote.ComplaintResponse
import com.techrise.data.remote.NewsResponse
import com.techrise.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    viewModel: AdminViewModel,
    onComplaintClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val complaints by viewModel.complaints.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Feed, 1: Publish News, 2: Escalations

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tech Rise - Staff Portal",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Feed") },
                    label = { Text("Complaint Feed") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Publish") },
                    label = { Text("Publish News") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Warning, contentDescription = "Escalations") },
                    label = { Text("Escalations") }
                )
            }
        }
    ) { paddingValues ->
        var isRefreshing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    viewModel.loadAllComplaints()
                    viewModel.loadNewsList()
                    kotlinx.coroutines.delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> ComplaintFeedTab(
                        complaints = complaints,
                        isLoading = isLoading,
                        error = error,
                        onComplaintClick = onComplaintClick,
                        onRefresh = { viewModel.loadAllComplaints() }
                    )
                    1 -> PublishNewsTab(
                        viewModel = viewModel,
                        isLoading = isLoading,
                        error = error
                    )
                    2 -> EscalationsTab(
                        complaints = complaints,
                        viewModel = viewModel,
                        onComplaintClick = onComplaintClick,
                        isLoading = isLoading,
                        error = error,
                        onRefresh = { viewModel.loadAllComplaints() }
                    )
                }
            }
        }
    }
}

@Composable
fun ComplaintFeedTab(
    complaints: List<ComplaintResponse>,
    isLoading: Boolean,
    error: String?,
    onComplaintClick: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, IN_PROGRESS, RESOLVED

    val filteredComplaints = remember(complaints, statusFilter) {
        if (statusFilter == "ALL") complaints
        else complaints.filter { it.status.uppercase() == statusFilter }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status filter chips
        ScrollableTabRow(
            selectedTabIndex = when (statusFilter) {
                "ALL" -> 0
                "PENDING" -> 1
                "IN_PROGRESS" -> 2
                "RESOLVED" -> 3
                else -> 0
            },
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Tab(selected = statusFilter == "ALL", onClick = { statusFilter = "ALL" }) {
                Text(
                    text = "All (${complaints.size})",
                    color = if (statusFilter == "ALL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = if (statusFilter == "ALL") FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Tab(selected = statusFilter == "PENDING", onClick = { statusFilter = "PENDING" }) {
                val count = complaints.count { it.status.uppercase() == "PENDING" }
                Text(
                    text = "Pending ($count)",
                    color = if (statusFilter == "PENDING") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = if (statusFilter == "PENDING") FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Tab(selected = statusFilter == "IN_PROGRESS", onClick = { statusFilter = "IN_PROGRESS" }) {
                val count = complaints.count { it.status.uppercase() == "IN_PROGRESS" }
                Text(
                    text = "Active ($count)",
                    color = if (statusFilter == "IN_PROGRESS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = if (statusFilter == "IN_PROGRESS") FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Tab(selected = statusFilter == "RESOLVED", onClick = { statusFilter = "RESOLVED" }) {
                val count = complaints.count { it.status.uppercase() == "RESOLVED" }
                Text(
                    text = "Resolved ($count)",
                    color = if (statusFilter == "RESOLVED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = if (statusFilter == "RESOLVED") FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (isLoading && filteredComplaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null && filteredComplaints.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                Button(onClick = onRefresh) { Text("Retry") }
            }
        } else if (filteredComplaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No complaints found for this status.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredComplaints) { complaint ->
                    AdminComplaintCard(complaint = complaint, onClick = { onComplaintClick(complaint.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishNewsTab(
    viewModel: AdminViewModel,
    isLoading: Boolean,
    error: String?
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val isSuccess by viewModel.newsPublishedSuccess.collectAsState()
    val newsList by viewModel.newsList.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            showDialog = true
            title = ""
            content = ""
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; viewModel.resetNewsPublishSuccess() },
            title = { Text("Success") },
            text = { Text("Official bulletin published successfully to the news feed!") },
            confirmButton = {
                TextButton(onClick = { showDialog = false; viewModel.resetNewsPublishSuccess() }) {
                    Text("OK")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Publish Company News",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Broadcast important company news, service notices, or security bulletins to all customer accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("News Title") },
                    placeholder = { Text("e.g. Server Maintenance Notice") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Bulletin Content") },
                    placeholder = { Text("Provide details of the announcement here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(bottom = 16.dp)
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = {
                        viewModel.publishNews(title, content) {}
                    },
                    enabled = title.isNotBlank() && content.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Publish Bulletin")
                    }
                }
            }
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = "Published Bulletins",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (newsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No bulletins published yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(newsList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.deleteNews(item.id) },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Bulletin",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EscalationsTab(
    complaints: List<ComplaintResponse>,
    viewModel: AdminViewModel,
    onComplaintClick: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    val escalatedComplaints = remember(complaints) {
        complaints.filter { viewModel.isComplaintEscalated(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Stagnant Issues Monitor",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "The following complaints have been open and unresolved for more than 7 days. These issues trigger an active visual warning alert.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading && escalatedComplaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null && escalatedComplaints.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRefresh) { Text("Retry") }
            }
        } else if (escalatedComplaints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Secure",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All clear! No issues are currently stagnant.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(escalatedComplaints) { complaint ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onComplaintClick(complaint.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = complaint.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "⚠️ ESCALATED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = complaint.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Client: ${com.techrise.util.formatEmailToName(complaint.customerEmail ?: complaint.customerId)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                val dateStr = complaint.createdAt?.let {
                                    val date = Date(it._seconds * 1000)
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                                } ?: "N/A"
                                Text(
                                    text = "Raised: $dateStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminComplaintCard(
    complaint: ComplaintResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = complaint.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ComplaintStatusBadge(status = complaint.status)
            }
            Spacer(modifier = Modifier.height(2.dp))
            val clientText = com.techrise.util.formatEmailToName(complaint.customerEmail ?: complaint.customerId)
            Text(
                text = "Client: $clientText",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = complaint.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            if (complaint.status.uppercase() == "RESOLVED" && complaint.rating != null && complaint.rating > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Client Rating: ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                                (1..5).forEach { index ->
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (index <= complaint.rating) Color(0xFFFFD700) else Color.LightGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "(${complaint.rating}/5)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (!complaint.feedbackComment.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${complaint.feedbackComment}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Priority: ${complaint.priority.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when (complaint.priority.uppercase()) {
                            "HIGH" -> PriorityHigh
                            "MEDIUM" -> PriorityMedium
                            else -> PriorityLow
                        }
                    )
                    val staffText = com.techrise.util.formatEmailToName(complaint.assignedAdminEmail ?: complaint.assignedAdminId)
                    Text(
                        text = "Assigned: $staffText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val dateStr = complaint.createdAt?.let {
                    val date = Date(it._seconds * 1000)
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                } ?: "N/A"
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ComplaintStatusBadge(status: String) {
    val (color, text) = when (status.uppercase()) {
        "PENDING" -> StatusPending to "Pending"
        "IN_PROGRESS" -> StatusActive to "Active"
        "RESOLVED" -> StatusResolved to "Resolved"
        else -> Color.Gray to status
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
