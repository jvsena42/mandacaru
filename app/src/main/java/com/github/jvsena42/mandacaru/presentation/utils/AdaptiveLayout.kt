package com.github.jvsena42.mandacaru.presentation.utils

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

data class AdaptiveLayout(
    val isMediumOrWider: Boolean,
    val isExpandedWidth: Boolean,
    val useRail: Boolean,
    val useTwoPane: Boolean,
    val horizontalPadding: Dp,
    val maxContentWidth: Dp,
)

@Composable
fun rememberAdaptiveLayout(): AdaptiveLayout {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    return remember(windowSizeClass) {
        val isMediumOrWider = windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
        )
        val isExpandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
        )
        val isMediumHeightOrTaller = windowSizeClass.isHeightAtLeastBreakpoint(
            WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
        )
        AdaptiveLayout(
            isMediumOrWider = isMediumOrWider,
            isExpandedWidth = isExpandedWidth,
            useRail = isMediumOrWider,
            useTwoPane = isExpandedWidth && isMediumHeightOrTaller,
            horizontalPadding = when {
                isExpandedWidth -> 32.dp
                isMediumOrWider -> 24.dp
                else -> 16.dp
            },
            maxContentWidth = if (isMediumOrWider) 1200.dp else 600.dp,
        )
    }
}
