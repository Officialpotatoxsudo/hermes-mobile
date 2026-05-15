package com.hermes.mobile.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hermes.mobile.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f))
        scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 200f))
        delay(1000)
        onFinished()
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.welcome))
    val progress by animateLottieCompositionAsState(composition, isPlaying = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition,
            { progress },
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer(alpha = alpha.value * 0.5f),
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer(
                    alpha = alpha.value,
                    scaleX = 0.5f + scale.value * 0.5f,
                    scaleY = 0.5f + scale.value * 0.5f,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), RoundedCornerShape(30.dp)),
            )
            Image(
                painter = painterResource(id = R.drawable.hermes_mark),
                contentDescription = "Hermes Logo",
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
