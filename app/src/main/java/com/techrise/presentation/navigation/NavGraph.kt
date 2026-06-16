package com.techrise.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.techrise.presentation.auth.AuthScreen
import com.techrise.presentation.auth.AuthViewModel
import com.techrise.presentation.complaints.*

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object CustomerHome : Screen("customer_home")
    object CreateTicket : Screen("create_ticket")
    object TicketDetails : Screen("ticket_details/{ticketId}") {
        fun createRoute(ticketId: String) = "ticket_details/$ticketId"
    }
}

@Composable
fun TechRiseNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = { role ->
                    // For Day 5, we route both Customers and Employees to CustomerHome.
                    // On Day 6, we will hook the Admin (Employee) Dashboard route here.
                    navController.navigate(Screen.CustomerHome.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CustomerHome.route) {
            val customerViewModel: CustomerViewModel = hiltViewModel()
            CustomerHomeScreen(
                viewModel = customerViewModel,
                onCreateTicketClick = {
                    navController.navigate(Screen.CreateTicket.route)
                },
                onTicketClick = { ticketId ->
                    navController.navigate(Screen.TicketDetails.createRoute(ticketId))
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.CustomerHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateTicket.route) {
            val customerViewModel: CustomerViewModel = hiltViewModel()
            CreateComplaintScreen(
                viewModel = customerViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TicketDetails.route) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId") ?: ""
            val customerViewModel: CustomerViewModel = hiltViewModel()
            ComplaintDetailScreen(
                complaintId = ticketId,
                viewModel = customerViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
