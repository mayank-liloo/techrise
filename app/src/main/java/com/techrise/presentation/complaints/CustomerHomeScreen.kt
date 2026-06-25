package com.techrise.presentation.complaints

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.techrise.presentation.theme.*
import kotlinx.coroutines.delay
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.techrise.data.remote.ComplaintResponse
import com.techrise.data.remote.NewsResponse
import com.techrise.data.remote.BannerResponse
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import java.text.SimpleDateFormat
import java.util.*

enum class CustomerScreen {
    DASHBOARD,
    COMPLAINTS,
    FEEDBACK,
    NEWS,
    SUPPORT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    viewModel: CustomerViewModel,
    onCreateComplaintClick: () -> Unit,
    onComplaintClick: (id: String) -> Unit,
    onLogout: () -> Unit
) {
    val complaintsState by viewModel.complaintsState.collectAsState()
    val newsState by viewModel.newsState.collectAsState()
    val bannersState by viewModel.bannersState.collectAsState()
    var currentScreen by remember { mutableStateOf(CustomerScreen.DASHBOARD) }

    LaunchedEffect(Unit) {
        viewModel.loadComplaints()
        viewModel.loadNewsFeed()
        viewModel.loadBanners()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val titleText = when (currentScreen) {
                            CustomerScreen.DASHBOARD -> "Tech Rise Portal"
                            CustomerScreen.COMPLAINTS -> "My Complaints"
                            CustomerScreen.FEEDBACK -> "Feedback Center"
                            CustomerScreen.NEWS -> "News Bulletins"
                            CustomerScreen.SUPPORT -> "Support Center"
                        }
                        Text(titleText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(viewModel.getEmail(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (currentScreen != CustomerScreen.DASHBOARD) {
                        IconButton(onClick = { currentScreen = CustomerScreen.DASHBOARD }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Dashboard")
                        }
                    }
                },
                actions = {
                    if (currentScreen == CustomerScreen.DASHBOARD) {
                        IconButton(onClick = {
                            viewModel.logout()
                            onLogout()
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Log Out")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            if (currentScreen == CustomerScreen.COMPLAINTS) {
                ExtendedFloatingActionButton(
                    onClick = onCreateComplaintClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Complaint") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                    viewModel.loadComplaints()
                    viewModel.loadNewsFeed()
                    viewModel.loadBanners()
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
                when (currentScreen) {
                CustomerScreen.DASHBOARD -> DashboardContent(
                    viewModel = viewModel,
                    complaintsCount = (complaintsState as? ComplaintsUiState.Success)?.complaints?.size ?: 0,
                    activeComplaintsCount = (complaintsState as? ComplaintsUiState.Success)?.complaints?.count { it.status != "RESOLVED" } ?: 0,
                    newsCount = newsState.size,
                    banners = bannersState,
                    onNavigate = { screen -> currentScreen = screen }
                )
                CustomerScreen.COMPLAINTS -> TrackerTabContent(
                    state = complaintsState,
                    onComplaintClick = onComplaintClick,
                    onRetry = { viewModel.loadComplaints() }
                )
                CustomerScreen.NEWS -> NewsTabContent(news = newsState)
                CustomerScreen.SUPPORT -> SupportTabContent()
                CustomerScreen.FEEDBACK -> FeedbackScreenContent(
                    viewModel = viewModel,
                    complaints = (complaintsState as? ComplaintsUiState.Success)?.complaints ?: emptyList()
                )
            }
            }
        }
    }
}

data class SlideData(
    val title: String,
    val description: String,
    val background: Brush,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val imageRes: Int? = null,
    val imageBitmap: ImageBitmap? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SlidingBanner(banners: List<BannerResponse>) {
    val slides = if (banners.isNotEmpty()) {
        banners.map { banner ->
            val bitmap = try {
                val cleanBase64 = if (banner.imageBase64.contains(",")) {
                    banner.imageBase64.substringAfter(",")
                } else {
                    banner.imageBase64
                }
                val byteArray = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val androidBitmap = android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                androidBitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }

            SlideData(
                title = banner.title,
                description = "",
                background = Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
                icon = Icons.Default.Send,
                imageRes = null,
                imageBitmap = bitmap
            )
        }
    } else {
        listOf(
            SlideData(
                title = "Banner 1",
                description = "",
                background = Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
                icon = Icons.Default.Send,
                imageRes = null
            ),
            SlideData(
                title = "Banner 2",
                description = "",
                background = Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
                icon = Icons.Default.Phone,
                imageRes = null
            ),
            SlideData(
                title = "Banner 3",
                description = "",
                background = Brush.linearGradient(listOf(Color(0xFFFC4A1A), Color(0xFFF7B733))),
                icon = Icons.Default.Star,
                imageRes = null
            ),
            SlideData(
                title = "Banner 4",
                description = "",
                background = Brush.linearGradient(listOf(Color(0xFF141E30), Color(0xFF243B55))),
                icon = Icons.Default.Notifications,
                imageRes = null
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Auto-scroll effect that pauses when manual swipe is in progress
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            if (!pagerState.isScrollInProgress) {
                if (pagerState.pageCount > 0) {
                    val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                    pagerState.animateScrollToPage(nextPage)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 60.dp),
            pageSpacing = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) { page ->
            val data = slides[page]
            
            // Calculate scale and alpha based on page offset to create beautiful peeking cards
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = lerp(
                start = 0.85f,
                stop = 1f,
                fraction = 1f - pageOffset.coerceIn(0f, 1f)
            )
            val alpha = lerp(
                start = 0.6f,
                stop = 1f,
                fraction = 1f - pageOffset.coerceIn(0f, 1f)
            )

            Card(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(data.background)
                    ) {
                        if (data.imageBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = data.imageBitmap,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (data.imageRes != null) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = data.imageRes),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Overlay for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Black.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                        )
                    }

                    // Content layout inside the card
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                imageVector = data.icon,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = data.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = data.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    info: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = info,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DashboardContent(
    viewModel: CustomerViewModel,
    complaintsCount: Int,
    activeComplaintsCount: Int,
    newsCount: Int,
    banners: List<BannerResponse>,
    onNavigate: (CustomerScreen) -> Unit
) {
    val username = viewModel.getEmail().substringBefore("@").replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Welcome back,",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = username,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SlidingBanner(banners = banners)

        Spacer(modifier = Modifier.height(16.dp))

        // Symmetric 2x2 grid layout
        // Row 1: Complaint & News
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "Complaint",
                subtitle = "Track & file",
                info = "$activeComplaintsCount Active",
                color = MaterialTheme.colorScheme.surfaceVariant,
                icon = Icons.Default.List,
                onClick = { onNavigate(CustomerScreen.COMPLAINTS) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            DashboardCard(
                title = "News",
                subtitle = "Latest announcements",
                info = "$newsCount Updates",
                color = MaterialTheme.colorScheme.surfaceVariant,
                icon = Icons.Default.Info,
                onClick = { onNavigate(CustomerScreen.NEWS) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Feedback & Support
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "Feedback",
                subtitle = "Rate resolved cases",
                info = "Help us improve",
                color = MaterialTheme.colorScheme.surfaceVariant,
                icon = Icons.Default.Star,
                onClick = { onNavigate(CustomerScreen.FEEDBACK) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            DashboardCard(
                title = "Support",
                subtitle = "FAQ & direct contact",
                info = "No: 8062179339",
                color = MaterialTheme.colorScheme.primaryContainer,
                icon = Icons.Default.Phone,
                onClick = { onNavigate(CustomerScreen.SUPPORT) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreenContent(
    viewModel: CustomerViewModel,
    complaints: List<ComplaintResponse>
) {
    val context = LocalContext.current
    var selectedComplaintId by remember { mutableStateOf<String?>(null) }
    var ratingInput by remember { mutableStateOf(0) }
    var commentInput by remember { mutableStateOf("") }

    val resolvedComplaints = complaints.filter { it.status == "RESOLVED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Feedback Center",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Help us improve our service by rating your resolved complaints.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (resolvedComplaints.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No resolved complaints available to rate yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(resolvedComplaints) { complaint ->
                val hasFeedback = complaint.rating != null && complaint.rating > 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasFeedback) 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else if (selectedComplaintId == complaint.id) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = complaint.title, 
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (hasFeedback) {
                                Surface(
                                    color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Submitted",
                                        color = Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        val resolvedDateStr = if (complaint.updatedAt != null) {
                            val date = Date(complaint.updatedAt._seconds * 1000)
                            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date)
                        } else {
                            "Just now"
                        }
                        Text(
                            text = "Resolved on: $resolvedDateStr", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (hasFeedback) {
                            Divider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Text(
                                text = "Your Rating:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            StarRating(
                                rating = complaint.rating ?: 0,
                                onRatingSelected = {} // Read-only
                            )
                            if (!complaint.feedbackComment.isNullOrBlank()) {
                                Text(
                                    text = "Your Comment:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "\"${complaint.feedbackComment}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        } else {
                            if (selectedComplaintId == complaint.id) {
                                Divider(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                Text(
                                    text = "Rate this support experience:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                StarRating(
                                    rating = ratingInput, 
                                    onRatingSelected = { ratingInput = it }
                                )
                                
                                OutlinedTextField(
                                    value = commentInput,
                                    onValueChange = { commentInput = it },
                                    label = { Text("Write your review comment...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    minLines = 2
                                )

                                Button(
                                    onClick = {
                                        viewModel.submitFeedback(
                                            id = complaint.id,
                                            rating = ratingInput,
                                            comment = commentInput
                                        ) { success, error ->
                                            if (success) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Feedback submitted successfully!",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                selectedComplaintId = null
                                                ratingInput = 0
                                                commentInput = ""
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Failed to submit feedback: $error",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    },
                                    enabled = ratingInput > 0,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    Text("Submit Feedback")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        selectedComplaintId = complaint.id
                                        ratingInput = 0
                                        commentInput = ""
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Rate Support Service")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StarRating(
    rating: Int,
    onRatingSelected: (Int) -> Unit
) {
    Row {
        (1..5).forEach { index ->
            IconButton(onClick = { onRatingSelected(index) }) {
                Icon(
                    imageVector = if (index <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (index <= rating) Color(0xFFFFD700) else Color.Gray
                )
            }
        }
    }
}

@Composable
fun NewsTabContent(news: List<NewsResponse>) {
    if (news.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No news updates at this time.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SupportTabContent() {
    val faqs = listOf(
        Pair("How do I raise a complaint?", "Tap the 'New Complaint' button on the Tracker screen. Provide a clear title, description, and assign the priority level. Submit, and our team will be notified immediately."),
        Pair("What are the priority levels?", "We resolve HIGH priority issues within 24 hours. MEDIUM priority targets 48 hours, and LOW priority targets 72 hours."),
        Pair("How can I track the complaint resolution?", "Select any complaint from your Tracker list. A visual timeline is generated detailing when it was registered, when staff members were assigned, and when it is completed.")
    )

    val context = LocalContext.current
    val supportPhone = "8062179339"

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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                        context.startActivity(intent)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Contact Support Line",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "8062179339",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                                context.startActivity(intent)
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call Support"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
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

@Composable
fun TrackerTabContent(
    state: ComplaintsUiState,
    onComplaintClick: (id: String) -> Unit,
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
                        "No complaints found. Tap 'New Complaint' to submit one.",
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
                    items(state.complaints) { complaint ->
                        ComplaintCard(complaint = complaint, onClick = { onComplaintClick(complaint.id) })
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
                    Text("Priority: ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
