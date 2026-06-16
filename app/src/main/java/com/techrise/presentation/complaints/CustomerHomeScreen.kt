package com.techrise.presentation.complaints

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Phone
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    viewModel: CustomerViewModel,
    onCreateTicketClick: () -> Unit,
    onTicketClick: (id: String) -> Unit,
    onLogout: () -> Unit
) {
    val complaintsState by viewModel.complaintsState.collectAsState()
    val newsState by viewModel.newsState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadComplaints()
        viewModel.loadNewsFeed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tech Rise Portal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(viewModel.getEmail(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Tracker") },
                    label = { Text("Tracker") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "News") },
                    label = { Text("News") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Phone, contentDescription = "Support") },
                    label = { Text("Support") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onCreateTicketClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Ticket") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> TrackerTabContent(
                    state = complaintsState,
                    onTicketClick = onTicketClick,
                    onRetry = { viewModel.loadComplaints() }
                )
                1 -> NewsTabContent(news = newsState)
                2 -> SupportTabContent()
            }
        }
    }
}

// --- TRACKER TAB CONTENT ---

@Composable
fun TrackerTabContent(
    state: ComplaintsUiState,
    onTicketClick: (id: String) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is ComplaintsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ComplaintsUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
        is ComplaintsUiState.Success -> {
            if (state.complaints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No complaints found. Tap 'New Ticket' to submit one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.complaints) { complaint ->
                        ComplaintCard(complaint = complaint, onClick = { onTicketClick(complaint.id) })
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun ComplaintCard(complaint: ComplaintResponse, onClick: () -> Unit) {
    val statusColor = when (complaint.status.toUpperCase()) {
        "PENDING" -> MaterialTheme.colorScheme.error
        "IN_PROGRESS" -> Color(0xFFE65100) // Deep Orange
        "RESOLVED" -> Color(0xFF2E7D32) // Green
        else -> MaterialTheme.colorScheme.outline
    }

    val priorityColor = when (complaint.priority.toUpperCase()) {
        "HIGH" -> MaterialTheme.colorScheme.error
        "MEDIUM" -> Color(0xFFF57C00)
        "LOW" -> Color(0xFF757575)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
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
                    modifier = Modifier.weight(1f)
                )
                
                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = complaint.status,
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = complaint.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Priority: ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = complaint.priority,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }

                // Date formatting
                val dateStr = if (complaint.createdAt != null) {
                    val date = Date(complaint.createdAt._seconds * 1000)
                    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
                } else {
                    "Just now"
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// --- NEWS TAB CONTENT ---

@Composable
fun NewsTabContent(news: List<NewsResponse>) {
    if (news.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No news updates at this time.", color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(news) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val date = Date(item.createdAt._seconds * 1000)
                        val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                        
                        Text(
                            text = "Published $formattedDate",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// --- SUPPORT TAB CONTENT ---

@Composable
fun SupportTabContent() {
    val faqs = listOf(
        Pair("How do I raise a complaint ticket?", "Tap the 'New Ticket' button on the Tracker screen. Provide a clear title, description, and assign the priority level. Submit, and our team will be notified immediately."),
        Pair("What are the priority levels?", "We resolve HIGH priority issues within 24 hours. MEDIUM priority targets 48 hours, and LOW priority targets 72 hours."),
        Pair("How can I track the ticket resolution?", "Select any ticket from your Tracker list. A visual timeline is generated detailing when it was registered, when staff members were assigned, and when it is completed.")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Direct support cards
        item {
            Text(
                text = "Contact Support",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Direct Hotline",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "+1 (800) 555-RISE (Toll-Free)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Support Email",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "support@techrise.com",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // FAQs Section
        item {
            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
        }

        items(faqs) { faq ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = faq.first,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = faq.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
