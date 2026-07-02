package com.techrise.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.techrise.presentation.auth.*
import com.techrise.presentation.complaints.*
import com.techrise.presentation.admin.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object CustomerHome : Screen("customer_home")
    object CreateComplaint : Screen("create_complaint")
    object ComplaintDetails : Screen("complaint_details/{complaintId}") {
        fun createRoute(complaintId: String) = "complaint_details/$complaintId"
    }
    object AdminHome : Screen("admin_home")
    object AdminComplaintDetails : Screen("admin_complaint_details/{complaintId}") {
        fun createRoute(complaintId: String) = "admin_complaint_details/$complaintId"
    }
}

@Composable
fun TechRiseNavGraph(
    repository: com.techrise.data.repository.TechRiseRepository,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                repository = repository,
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToCustomerHome = {
                    navController.navigate(Screen.CustomerHome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToAdminHome = {
                    navController.navigate(Screen.AdminHome.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = authViewModel,
                onAuthSuccess = { role ->
                    if (role.uppercase() == "ADMIN") {
                        navController.navigate(Screen.AdminHome.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.CustomerHome.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.CustomerHome.route) { backStackEntry ->
            val customerViewModel: CustomerViewModel = hiltViewModel(backStackEntry)
            CustomerHomeScreen(
                viewModel = customerViewModel,
                onCreateComplaintClick = {
                    navController.navigate(Screen.CreateComplaint.route)
                },
                onComplaintClick = { complaintId ->
                    navController.navigate(Screen.ComplaintDetails.createRoute(complaintId))
                },
                onLogout = {
                    customerViewModel.logout()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.CustomerHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateComplaint.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.CustomerHome.route)
            }
            val customerViewModel: CustomerViewModel = hiltViewModel(parentEntry)
            CreateComplaintScreen(
                viewModel = customerViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ComplaintDetails.route) { backStackEntry ->
            val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.CustomerHome.route)
            }
            val customerViewModel: CustomerViewModel = hiltViewModel(parentEntry)
            ComplaintDetailScreen(
                complaintId = complaintId,
                viewModel = customerViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AdminHome.route) { backStackEntry ->
            val adminViewModel: AdminViewModel = hiltViewModel(backStackEntry)
            AdminHomeScreen(
                viewModel = adminViewModel,
                onComplaintClick = { complaintId ->
                    navController.navigate(Screen.AdminComplaintDetails.createRoute(complaintId))
                },
                onLogout = {
                    adminViewModel.logout()
                    adminViewModel.clearError()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.AdminHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminComplaintDetails.route) { backStackEntry ->
            val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.AdminHome.route)
            }
            val adminViewModel: AdminViewModel = hiltViewModel(parentEntry)
            AdminComplaintDetailsScreen(
                complaintId = complaintId,
                viewModel = adminViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
