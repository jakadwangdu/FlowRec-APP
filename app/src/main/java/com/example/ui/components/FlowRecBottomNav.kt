package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.Screen

@Composable
fun FlowRecBottomNav(
    currentTab: Screen,
    onTabSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Rounded Minimalist Capsule Dock (Text Removed, Smooth Animation, Ultra Fast)
        Box(
            modifier = Modifier
                .shadow(18.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavIconItem(
                    contentDescription = "Home",
                    selected = currentTab == Screen.HOME,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onTabSelected(Screen.HOME) },
                    testTag = "nav_tab_home"
                )

                BottomNavIconItem(
                    contentDescription = "Library",
                    selected = currentTab == Screen.LIBRARY,
                    selectedIcon = Icons.Filled.VideoLibrary,
                    unselectedIcon = Icons.Outlined.VideoLibrary,
                    onClick = { onTabSelected(Screen.LIBRARY) },
                    testTag = "nav_tab_library"
                )

                BottomNavIconItem(
                    contentDescription = "Settings",
                    selected = currentTab == Screen.SETTINGS,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    onClick = { onTabSelected(Screen.SETTINGS) },
                    testTag = "nav_tab_settings"
                )
            }
        }
    }
}

@Composable
private fun BottomNavIconItem(
    contentDescription: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Spring bounce scale animation for selected icon
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.22f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dock_icon_scale"
    )

    // Smooth color tint transition
    val iconTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "dock_icon_tint"
    )

    // Animated capsule pill background behind the active icon
    val pillBgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "dock_pill_bg"
    )

    // Subtle animated indicator dot scale
    val indicatorScale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dock_indicator_scale"
    )

    Column(
        modifier = Modifier
            .size(width = 54.dp, height = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(pillBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = onClick
            )
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )

        // Micro-indicator dot
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(4.dp)
                .graphicsLayer {
                    scaleX = indicatorScale
                    scaleY = indicatorScale
                    alpha = indicatorScale
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
