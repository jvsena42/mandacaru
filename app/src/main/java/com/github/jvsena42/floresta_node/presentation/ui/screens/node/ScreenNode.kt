package com.github.jvsena42.floresta_node.presentation.ui.screens.node

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.jvsena42.floresta_node.R
import com.github.jvsena42.floresta_node.presentation.ui.theme.FlorestaNodeTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScreenNode(
    viewModel: NodeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    ScreenNode(uiState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenNode(uiState: NodeUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    stringResource(R.string.node),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        stringResource(R.string.network),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        uiState.network,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.block_height),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    AnimatedContent(
                        targetState = uiState.blockHeight,
                        label = stringResource(R.string.best_block)
                    ) { animated ->
                        Text(
                            animated,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.number_of_peers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )


                    AnimatedVisibility(visible = uiState.numberOfPeers.isNotEmpty()) {
                        Text(
                            uiState.numberOfPeers,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    AnimatedVisibility(visible = uiState.numberOfPeers.isEmpty()) {
                        LinearProgressIndicator(modifier = Modifier.width(100.dp))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.validated_blocks),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        uiState.validatedBLocks.toString(),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.best_block),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    AnimatedContent(
                        targetState = uiState.blockHash,
                        label = stringResource(R.string.best_block)
                    ) { animated ->
                        Text(
                            animated,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.difficulty),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )


                    Text(
                        uiState.difficulty,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.sync),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        "${uiState.syncPercentage}%",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    FlorestaNodeTheme {
        ScreenNode(
            NodeUiState(
                numberOfPeers = "5",
                blockHeight = "1235334",
                blockHash = "00000cb40a568e8da8a045ced110137e159f890ac4da883b6b17dc651b3a8049",
                network = "Signet",
                difficulty = "9.7"
            )
        )
    }
}