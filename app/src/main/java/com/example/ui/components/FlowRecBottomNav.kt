package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.UiFeatureFlagManager
import com.example.ui.theme.flow
import com.example.ui.viewmodel.Screen

@Composable
fun FlowRecBottomNav(
    currentTab: Screen,
    onTabSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val featureFlags by UiFeatureFlagManager.getInstance(context).flags.collectAsState()
    val isMinimalist = featureFlags.useMinimalistMonochromeUi

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = if (isMinimalist) 20.dp else 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isMinimalist) {
            // FlowRec Minimalist Floating Glass Dock (4 Tabs: Home, Capture, Library, Settings)
            Row(
                modifier = Modifier
                    .shadow(14.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .background(flow.glass)
                    .border(0.75.dp, flow.separator, CircleShape)
                    .padding(5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. HOME
                FlowDockItem(
                    label = "Home",
                    icon = Icons.Outlined.Home,
                    activeIcon = Icons.Filled.Home,
                    selected = currentTab == Screen.HOME,
                    onClick = { onTabSelected(Screen.HOME) },
                    testTag = "nav_tab_home"
                )

                // 2. CAPTURE / RECORD
                FlowDockItem(
                    label = "Capture",
                    icon = Icons.Outlined.Videocam,
                    activeIcon = Icons.Filled.Videocam,
                    selected = currentTab == Screen.RECORD || currentTab == Screen.NEW_RECORDING,
                    onClick = { onTabSelected(Screen.RECORD) },
                    testTag = "nav_tab_record"
                )

                // 3. LIBRARY / PROJECTS
                FlowDockItem(
                    label = "Library",
                    icon = Icons.Outlined.VideoLibrary,
                    activeIcon = Icons.Filled.VideoLibrary,
                    selected = currentTab == Screen.PROJECTS || currentTab == Screen.LIBRARY,
                    onClick = { onTabSelected(Screen.PROJECTS) },
                    testTag = "nav_tab_projects"
                )

                // 4. SETTINGS
                FlowDockItem(
                    label = "Settings",
                    icon = Icons.Outlined.Settings,
                    activeIcon = Icons.Filled.Settings,
                    selected = currentTab == Screen.SETTINGS,
                    onClick = { onTabSelected(Screen.SETTINGS) },
                    testTag = "nav_tab_settings"
                )
            }
        } else {
            // Classic Floating Capsule Dock Container (5 tabs)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.45f))
                    .clip(RoundedCornerShape(26.dp))
                    .background(flow.glass)
                    .border(1.dp, flow.separator, RoundedCornerShape(26.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClassicBottomNavItem(
                        label = "Home",
                        selected = currentTab == Screen.HOME,
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        onClick = { onTabSelected(Screen.HOME) },
                        testTag = "nav_tab_home"
                    )
                    ClassicBottomNavItem(
                        label = "Record",
                        selected = currentTab == Screen.RECORD || currentTab == Screen.NEW_RECORDING,
                        selectedIcon = Icons.Filled.Videocam,
                        unselectedIcon = Icons.Outlined.Videocam,
                        isRecordTab = true,
                        onClick = { onTabSelected(Screen.RECORD) },
                        testTag = "nav_tab_record"
                    )
                    ClassicBottomNavItem(
                        label = "Projects",
                        selected = currentTab == Screen.PROJECTS || currentTab == Screen.LIBRARY,
                        selectedIcon = Icons.Filled.VideoLibrary,
                        unselectedIcon = Icons.Outlined.VideoLibrary,
                        onClick = { onTabSelected(Screen.PROJECTS) },
                        testTag = "nav_tab_projects"
                    )
                    ClassicBottomNavItem(
                        label = "Editor",
                        selected = currentTab == Screen.EDITOR,
                        selectedIcon = Icons.Filled.Edit,
                        unselectedIcon = Icons.Outlined.Edit,
                        onClick = { onTabSelected(Screen.EDITOR) },
                        testTag = "nav_tab_editor"
                    )
                    ClassicBottomNavItem(
                        label = "Settings",
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
}

/**
 * Minimalist Dock Item (64 x 48 dp, pill selection, subtle haptics)
 * Defined in files/compose.md
 */
@Composable
private fun FlowDockItem(
    label: String,
    icon: ImageVector,
    activeIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val bgTint by animateColorAsState(
        targetValue = if (selected) flow.fill else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "dock_bg_tint"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) flow.fg else flow.muted,
        animationSpec = tween(durationMillis = 200),
        label = "dock_icon_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "dock_icon_scale"
    )

    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 48.dp)
            .clip(CircleShape)
            .background(bgTint)
            .semantics {
                role = Role.Tab
                contentDescription = label
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (e: Exception) {
                        // Ignore
                    }
                    onClick()
                }
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (selected) activeIcon else icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

@Composable
private fun ClassicBottomNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isRecordTab: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint = if (selected) flow.fg else flow.muted

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (isRecordTab && selected) flow.record else tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
            ),
            color = if (isRecordTab && selected) flow.record else tint,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
