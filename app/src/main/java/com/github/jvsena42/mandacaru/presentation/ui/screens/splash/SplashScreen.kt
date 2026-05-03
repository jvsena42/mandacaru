package com.github.jvsena42.mandacaru.presentation.ui.screens.splash

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.jvsena42.mandacaru.R
import com.github.jvsena42.mandacaru.presentation.ui.theme.MandacaruTheme

private val SplashBackground = Color(0xFFFEB22C)

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    var animateIn by remember { mutableStateOf(false) }

    val iconScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.6f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "splash-icon-scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "splash-content-alpha"
    )

    LaunchedEffect(Unit) {
        animateIn = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(192.dp)
                    .scale(iconScale)
                    .alpha(contentAlpha),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF462D00),
                modifier = Modifier.alpha(contentAlpha),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF462D00),
                modifier = Modifier.alpha(contentAlpha),
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Tablet", widthDp = 840, heightDp = 1280)
@Composable
private fun SplashScreenPreview() {
    MandacaruTheme {
        SplashScreen()
    }
}
