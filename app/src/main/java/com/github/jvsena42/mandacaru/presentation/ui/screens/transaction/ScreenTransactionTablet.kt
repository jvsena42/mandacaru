package com.github.jvsena42.mandacaru.presentation.ui.screens.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.dp

@Composable
internal fun TransactionTabletDashboard(
    uiState: TransactionUiState,
    onAction: (TransactionAction) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = uiState.isSearchLoading || uiState.isBroadcasting,
            enter = progressEnterTransition(),
            exit = progressExitTransition(),
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1.6f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TransactionLookupCard(
                    uiState = uiState,
                    onAction = onAction,
                    focusManager = focusManager,
                )
                AnimatedContent(
                    targetState = uiState.searchResult?.result,
                    label = "tx_details",
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            initialOffsetY = { it / 6 },
                        ) + fadeIn(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        )) togetherWith (slideOutVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            targetOffsetY = { -it / 8 },
                        ) + fadeOut(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        ))
                    },
                ) { tx ->
                    if (tx != null) {
                        TransactionDetailsCard(tx)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BroadcastTransactionCard(
                    uiState = uiState,
                    onAction = onAction,
                )
            }
        }
    }
}
