package com.techrise.presentation.complaints

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateComplaintScreen(
    viewModel: CustomerViewModel,
    onBack: () -> Unit
) {
    val createState by viewModel.createTicketState.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var showPriorityDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(createState) {
        if (createState is CreateTicketUiState.Success) {
            viewModel.resetCreateState()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Ticket", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Explain the issue you are experiencing",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Ticket Title") },
                placeholder = { Text("e.g. Broken portal links") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Detailed Description") },
                placeholder = { Text("Describe the issue, error codes, and steps to reproduce...") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 6,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Dropdown Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = priority,
                    onValueChange = {},
                    label = { Text("Ticket Priority") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            Modifier.clickable { showPriorityDropdown = true }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showPriorityDropdown = true },
                    shape = RoundedCornerShape(8.dp)
                )

                DropdownMenu(
                    expanded = showPriorityDropdown,
                    onDismissRequest = { showPriorityDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("LOW") },
                        onClick = {
                            priority = "LOW"
                            showPriorityDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("MEDIUM") },
                        onClick = {
                            priority = "MEDIUM"
                            showPriorityDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("HIGH") },
                        onClick = {
                            priority = "HIGH"
                            showPriorityDropdown = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    viewModel.createComplaint(title.trim(), description.trim(), priority)
                },
                enabled = title.isNotBlank() && description.isNotBlank() && createState !is CreateTicketUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (createState is CreateTicketUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Submit Ticket", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Error Display
            if (createState is CreateTicketUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (createState as CreateTicketUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
