package com.example.homenotes

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/*
 * "Desk" scene for the Home-info screen header: the mascot sits at a cabin desk
 * writing a note.
 *
 * Robust framing: the three layers (desk_base, desk_hand, desk_light) are all the
 * SAME full-canvas image size and are drawn with the exact same ContentScale.Crop +
 * TopCenter. Because Compose transforms them identically, the writing hand and the
 * warm-light overlay always stay pixel-aligned with the base on every device width —
 * no manual offset/scale math (which previously coerced the oversized base and let the
 * hand drift). Crop+TopCenter keeps the mascot's head in view (top of the artwork)
 * regardless of width; wider screens simply zoom in a little more.
 *
 * The hand layer rocks a few degrees around the wrist so the pencil "writes"; the
 * pivot is computed from the live Crop scale so it lands on the wrist on any width.
 */

// Native size of every layer canvas (the cabin artwork, top trimmed so the head sits
// near the top of the frame).
private const val CANVAS_W = 1254f
private const val CANVAS_H = 1104f
// Wrist position inside that canvas — the pivot the writing hand rocks around.
private const val WRIST_X = 491f
private const val WRIST_Y = 480f
private const val WRITE_ANGLE = 4f

private val DeskWood = Color(0xFF4A250C)

@Composable
internal fun HomeDeskHeader(
    modifier: Modifier = Modifier
) {
    val description = stringResource(R.string.home_info_header_description)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clipToBounds()
            .background(DeskWood)
            .semantics { contentDescription = description }
    ) {
        HomeDeskScene(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun HomeDeskScene(modifier: Modifier = Modifier) {
    val animationsEnabled = rememberDeskAnimationsEnabled()

    val rock = rememberInfiniteTransition(label = "desk_write")
    val rockAngle by rock.animateFloat(
        initialValue = -WRITE_ANGLE, targetValue = WRITE_ANGLE,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "write_angle"
    )
    val glow = rememberInfiniteTransition(label = "desk_glow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.45f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val angle = if (animationsEnabled) rockAngle else 0f
    val lightAlpha = if (animationsEnabled) glowAlpha else 0.6f

    BoxWithConstraints(modifier = modifier) {
        val boxW = maxWidth.value
        val boxH = maxHeight.value
        // Reproduce ContentScale.Crop (cover) + TopCenter to locate the wrist pivot.
        val coverScale = maxOf(boxW / CANVAS_W, boxH / CANVAS_H)
        val cropX0 = (CANVAS_W * coverScale - boxW) / 2f
        val pivotX = ((WRIST_X * coverScale - cropX0) / boxW).coerceIn(0f, 1f)
        val pivotY = ((WRIST_Y * coverScale) / boxH).coerceIn(0f, 1f)
        val wristPivot = TransformOrigin(pivotX, pivotY)

        Image(
            painter = painterResource(id = R.drawable.desk_base),
            contentDescription = null,
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(id = R.drawable.desk_hand),
            contentDescription = null,
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = angle
                    transformOrigin = wristPivot
                }
        )
        Image(
            painter = painterResource(id = R.drawable.desk_light),
            contentDescription = null,
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = lightAlpha }
        )
    }
}

@Composable
private fun rememberDeskAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) != 0f
        }.getOrDefault(true)
    }
}
