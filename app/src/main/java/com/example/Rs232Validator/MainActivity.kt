package com.example.Rs232Validator

import PTI.Rs232Validator.SerialProviders.FT311UARTInterface
import PTI.Rs232Validator.SerialProviders.ISerialProvider
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import com.example.Rs232Validator.ui.theme.Rs232ValidatorTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.Rs232Validator.Screens.*
import com.example.Rs232Validator.ViewModel.*


class MainActivity : ComponentActivity() {
    private lateinit var validatorViewModel: ValidatorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("Validator_prefs", Context.MODE_PRIVATE)

        validatorViewModel = ValidatorViewModel(application)


        var SerialProvider = FT311UARTInterface(this)
        validatorViewModel.initializeValidator(SerialProvider)

        enableEdgeToEdge()
        setContent {
            Rs232ValidatorTheme(dynamicColor = false, darkTheme = false) {
                val navController = rememberNavController()
                Surface(color = Color.White) {
                    Scaffold(
                        bottomBar = {
                            BottomNavigationBar(navController = navController)
                        }, content = { padding ->
                            NavHostContainer(navController = navController, padding = padding, viewModel = validatorViewModel)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NavHostContainer(
    navController: NavHostController,
    padding: PaddingValues,
    viewModel: ValidatorViewModel
){
    NavHost(
        navController = navController,
        startDestination = "polling",
        modifier = Modifier.padding(paddingValues = padding),
        builder = {
            composable("polling") {
                PollingScreen(viewModel = viewModel)
            }

            composable("states") {
                StatesScreen(viewModel = viewModel)
            }

            composable("telemetry") {
                TelemetryScreen(viewModel = viewModel)
            }

            composable("extended") {
                ExtendedScreen(viewModel = viewModel)
            }

            composable("logs") {
                LogsScreen(viewModel = viewModel)
            }
        }
    )
}

@Composable
fun BottomNavigationBar(navController: NavHostController){
    NavigationBar(
        containerColor = Color.Gray
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()

        val currentRoute = navBackStackEntry?.destination?.route

        Constants.BottomNavItems.forEach { navItem ->
            NavigationBarItem(
                selected = currentRoute == navItem.route,

                onClick = {
                    navController.navigate(navItem.route) {
                        launchSingleTop = true;
                        restoreState = true;
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true;
                        }
                    }
                },

                icon = {
                    Icon(imageVector = navItem.icon, contentDescription = navItem.label)
                },

                label = {
                    Text(text = navItem.label)
                },
                alwaysShowLabel = true,

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    unselectedIconColor = Color.Black,
                    selectedTextColor = Color.Black,
                    unselectedTextColor = Color.Black,
                    indicatorColor = Color.LightGray
                )
            )
        }
    }
}



