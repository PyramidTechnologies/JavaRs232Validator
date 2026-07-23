package com.example.Rs232Validator


import PTI.Rs232Validator.SerialProviders.SerialPort
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
import com.example.Rs232Validator.ui.theme.Rs232ValidatorTheme
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.Rs232Validator.Screens.*
import com.example.Rs232Validator.ViewModel.*


class MainActivity : ComponentActivity() {
    private lateinit var validatorViewModel: ValidatorViewModel
    private val serialProvider: SerialPort by lazy { SerialPort(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        validatorViewModel = ValidatorViewModel(application)

        validatorViewModel.initializeValidator(serialProvider)

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

    override fun onResume(){
        super.onResume()
        serialProvider.ResumeAccessory()
    }

    override fun onPause(){
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        validatorViewModel.billValidator.close()
        serialProvider.DestroyAccessory(serialProvider.isConfiged != 0x00.toByte())
        super.onDestroy()
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



