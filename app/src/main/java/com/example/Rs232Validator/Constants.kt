package com.example.Rs232Validator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Constants {
    val BottomNavItems = listOf(
        BottomNavItem(
            label = "Polling",
            icon = Icons.Filled.Home,
            route = "polling"
        ),

        BottomNavItem(
            label = "States",
            icon = Icons.Filled.Build,
            route = "states"
        ),

        BottomNavItem(
            label = "Telemetry",
            icon = Icons.Filled.Settings,
            route = "telemetry"
        ),

        BottomNavItem(
            label = "Extended",
            icon = Icons.Filled.Refresh,
            route = "extended"
        ),

        BottomNavItem(
            label = "Logs",
            icon = Icons.Filled.Notifications,
            route = "logs"
        )
    )

    val NeutralBackgroundTransparentRest: Color = Color(0x00FFFFFF)
    val NeutralStrokeDisabledRest: Color = Color(0xFFE0E0E0)
    val NeutralForegroundOnBrandRest: Color = Color(0xFFFFFFFF)

    val BrandBackground1Rest: Color = Color(0xFF0F6CBD)
    val Medium: Dp = 4.dp
}