package com.techrise.presentation.complaints

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.techrise.data.remote.ComplaintLogResponse
import com.techrise.data.remote.ComplaintResponse
import com.techrise.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import android.util.Base64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDetailScreen(
    complaintId: String,
    viewModel: CustomerViewModel,
    onBack: () -> Unit
) {
    val singleComplaintState by viewModel.singleComplaintState.collectAsState()

    var feedbackRating by remember { mutableIntStateOf(5) }
    var feedbackComment by remember { mutableStateOf("") }
    var feedbackSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(complaintId) {
        viewModel.loadComplaintDetails(complaintId)
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
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        var isRefreshing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    viewModel.loadComplaintDetails(complaintId)
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = singleComplaintState) {
                is SingleComplaintUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SingleComplaintUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is SingleComplaintUiState.Success -> {
                    val complaint = state.complaint
                    val logs = state.logs

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. Complaint Basic Details Card
                        item {
                            ComplaintDetailsCard(complaint = complaint)
                        }

                        // 2. Timeline Progression Section
                        item {
                            Text(
                                text = "Complaint Timeline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        if (logs.isEmpty()) {
                            item {
                                Text("No logs registered for this complaint.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            itemsIndexed(logs) { index, log ->
                                TimelineItem(
                                    log = log,
                                    isFirst = index == 0,
                                    isLast = index == logs.lastIndex
                                )
                            }
                        }

                        // 3. Customer Feedback Form (Show if RESOLVED)
                        if (complaint.status.toUpperCase() == "RESOLVED") {
                            item {
                                Divider(modifier = Modifier.padding(vertical = 12.dp))
                                Text(
                                    text = "Resolution Feedback",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                val hasSavedFeedback = complaint.rating != null
                                if (hasSavedFeedback || feedbackSubmitted) {
                                    // Display submitted feedback
                                    val finalRating = complaint.rating ?: feedbackRating
                                    val finalComment = complaint.feedbackComment ?: feedbackComment

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Your Rating: ", fontWeight = FontWeight.Bold)
                                                repeat(5) { starIndex ->
                                                    Icon(
                                                        imageVector = if (starIndex < finalRating) Icons.Default.Star else Icons.Outlined.Star,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFB300),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            if (!finalComment.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Comment: \"$finalComment\"", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                } else {
                                    // Render input feedback form
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("How would you rate our support resolution?", style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            // Star selector
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Start,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                repeat(5) { starIndex ->
                                                    val starValue = starIndex + 1
                                                    Icon(
                                                        imageVector = if (starValue <= feedbackRating) Icons.Default.Star else Icons.Outlined.Star,
                                                        contentDescription = null,
                                                        tint = Color(0xFFFFB300),
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clickable { feedbackRating = starValue }
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            OutlinedTextField(
                                                value = feedbackComment,
                                                onValueChange = { feedbackComment = it },
                                                label = { Text("Write a review comment (optional)") },
                                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                                maxLines = 3,
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            val context = LocalContext.current
                                            Button(
                                                onClick = {
                                                    viewModel.submitFeedback(
                                                        complaint.id,
                                                        feedbackRating,
                                                        feedbackComment.trim()
                                                    ) { success, error ->
                                                        if (success) {
                                                            feedbackSubmitted = true
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Feedback submitted successfully!",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Failed to submit feedback: $error",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Submit Feedback")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
}

@Composable
fun ComplaintDetailsCard(complaint: ComplaintResponse) {
    val statusColor = when (complaint.status.toUpperCase()) {
        "PENDING" -> StatusPending
        "IN_PROGRESS" -> StatusActive
        "RESOLVED" -> StatusResolved
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = complaint.status,
                        color = statusColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Description",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = complaint.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )
            if (!complaint.imageBase64.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Base64Image(
                    base64String = complaint.imageBase64,
                    contentDescription = "Complaint Attachment",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("PRIORITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(complaint.priority, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ASSIGNED STAFF", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val staffText = com.techrise.util.formatEmailToName(complaint.assignedAdminEmail ?: complaint.assignedAdminId)
                    Text(
                        text = staffText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (complaint.assignedAdminId == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineItem(
    log: ComplaintLogResponse,
    isFirst: Boolean,
    isLast: Boolean
) {
    val timelineColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Forces children columns to match parent row height
    ) {
        // Draw the vertical line and dot
        Box(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(x = size.width / 2, y = 16.dp.toPx())
                
                // Draw connecting line
                val startY = if (isFirst) center.y else 0f
                val endY = if (isLast) center.y else size.height
                drawLine(
                    color = timelineColor.copy(alpha = 0.38f),
                    start = Offset(center.x, startY),
                    end = Offset(center.x, endY),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw timeline indicator circle
                drawCircle(
                    color = timelineColor,
                    radius = 6.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }
        }

        // Timeline log details card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusText = if (log.oldStatus == "NONE") {
                        "Complaint Registered"
                    } else {
                        "Status: ${log.oldStatus} → ${log.newStatus}"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val date = Date(log.createdAt._seconds * 1000)
                    val formattedDate = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Actor: ${com.techrise.util.formatEmailToName(log.actionByEmail ?: log.actionBy)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Base64Image(base64String: String, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val cleanBase64 = if (base64String.contains(",")) {
        base64String.substringAfter(",")
    } else {
        base64String
    }
    val imageBytes = try {
        Base64.decode(cleanBase64, Base64.DEFAULT)
    } catch (e: Exception) {
        null
    }
    val bitmap = imageBytes?.let {
        BitmapFactory.decodeByteArray(it, 0, it.size)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
