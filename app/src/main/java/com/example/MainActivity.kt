package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.AppRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ClientDashboardScreen
import com.example.ui.screens.SimpleLoginScreen
import com.example.ui.screens.WorkerDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private val repository by lazy { AppRepository(applicationContext, database) }

    private val authViewModel: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(repository) as T
            }
        }
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = com.example.ui.theme.DarkAppBackground
                ) { innerPadding ->
                    RoleVaultApp(
                        authViewModel = authViewModel,
                        dashboardViewModel = dashboardViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleVaultApp(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        if (currentUser == null) {
            // "start met als enige scherm een inlogscherm.. waar 3 gebruikers tpes kan kiezen, Admin,Werker,Klant"
            SimpleLoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { user ->
                    // user is now authenticated and stored in repository.currentUser
                }
            )
        } else {
            // "en vervolgens doorstuurt naar een dashboard op basis van hun specifieke rol... en waarbij elke rol een uniek dashboard krijgt met op maat gemaakte functies"
            when (currentUser?.role) {
                UserRole.ADMIN -> {
                    // admin=hoogste authority
                    AdminDashboardScreen(
                        viewModel = dashboardViewModel,
                        onLogout = { authViewModel.logout() }
                    )
                }
                UserRole.WERKER -> {
                    // werker=toegang planning
                    WorkerDashboardScreen(
                        viewModel = dashboardViewModel,
                        onLogout = { authViewModel.logout() }
                    )
                }
                UserRole.KLANT -> {
                    // klant=beperkte leesrechten
                    ClientDashboardScreen(
                        viewModel = dashboardViewModel,
                        onLogout = { authViewModel.logout() }
                    )
                }
                null -> {
                    SimpleLoginScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = {}
                    )
                }
            }
        }
    }
}
