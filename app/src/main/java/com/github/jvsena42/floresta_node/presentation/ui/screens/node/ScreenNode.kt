package com.github.jvsena42.floresta_node.presentation.ui.screens.node

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.jvsena42.floresta_node.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScreenNode(
    viewModel: NodeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    ScreenNode(uiState)
}

private val cardHeight = 128.dp

private val cardsModifier = Modifier
    .height(height = cardHeight)
    .fillMaxWidth()
    .border(
        width = 1.dp,
        color = Color.Gray,
        shape = CircleShape.copy(CornerSize(24.dp))
    )
    .padding(horizontal = 12.dp, vertical = 14.dp)

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
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black
                )
            }
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = cardsModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.number_of_peers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    AnimatedVisibility(visible = uiState.numberOfPeers.isNotEmpty()) {
                        Text(uiState.numberOfPeers)
                    }

                    AnimatedVisibility(visible = uiState.numberOfPeers.isEmpty()) {
                        LinearProgressIndicator(modifier = Modifier.padding(horizontal = 32.dp))
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            item {
                Column(
                    modifier = cardsModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    Text(
                        stringResource(R.string.block_height),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    AnimatedContent(
                        targetState = uiState.blockHeight,
                        label = stringResource(R.string.best_block)
                    ) { animated ->
                        Text(
                            animated,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            item {
                Column(
                    modifier = cardsModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    Text(
                        stringResource(R.string.best_block),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    AnimatedContent(
                        targetState = uiState.blockHash,
                        label = stringResource(R.string.best_block)
                    ) { animated ->
                        Text(
                            animated,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            item {
                Column(
                    modifier = cardsModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    Text(
                        stringResource(R.string.network),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(uiState.network)

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            item {
                Column(
                    modifier = cardsModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.difficulty),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Text(uiState.difficulty)

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            item {
                Column(
                    modifier = cardsModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.sync),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text("${uiState.syncPercentage}%")

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            item {
                Column(
                    modifier = cardsModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.validated_blocks),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Text(uiState.validatedBLocks.toString())

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    MaterialTheme {
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