package gr.panoramapolihnitou.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.panoramapolihnitou.app.resources.Res
import gr.panoramapolihnitou.app.resources.logo
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Animated launch screen ported from the reference `SplashScreen.js`:
 * diagonal red gradient, logo fade/scale/slide-in, divider, tagline, footer.
 * Calls [onFinish] after ~2.8s.
 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val fade = remember { Animatable(0f) }
    val scale = remember { Animatable(0.85f) }
    val slide = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(800)) }
        launch { scale.animateTo(1f, tween(700)) }
        slide.animateTo(0f, tween(700))
        delay(2100)
        fade.animateTo(0f, tween(500))
        onFinish()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(PanoramaColors.primaryDark, PanoramaColors.primary, Color(0xFF8B0000)),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(fade.value)
                .scale(scale.value)
                .padding(bottom = slide.value.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = "Πανόραμα Πολιχνίτου",
                modifier = Modifier.width(260.dp).height(81.dp),
                contentScale = ContentScale.Fit
            )
            Box(
                Modifier
                    .padding(vertical = 16.dp)
                    .width(60.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color(0x80FFFFFF))
            )
            androidx.compose.material3.Text(
                text = "Η ενημέρωση της Λέσβου",
                color = Color(0xB3FFFFFF),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }

        androidx.compose.material3.Text(
            text = "panoramapolihnitou.gr",
            color = Color(0x80FFFFFF),
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).alpha(fade.value)
        )
    }
}
