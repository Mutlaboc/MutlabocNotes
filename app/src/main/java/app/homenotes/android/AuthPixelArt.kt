package app.homenotes.android

import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** Decode a pixel-art drawable once and keep it crisp (no resampling at use site). */
@Composable
internal fun pixelImageBitmap(drawableId: Int): ImageBitmap {
    val resources = LocalContext.current.resources
    return remember(drawableId, resources) {
        BitmapFactory.decodeResource(resources, drawableId).asImageBitmap()
    }
}

@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) != 0f
        }.getOrDefault(true)
    }
}


/* ------------------------------------------------------------------ */
/* Small pixel house drawn with Canvas (used for the top brand icon    */
/* and the cozy house in the bottom scene).                            */
/* ------------------------------------------------------------------ */

/* ------------------------------------------------------------------ */
/* Subtle sky decorations: tiny clouds, stars and sparkles.            */
/* ------------------------------------------------------------------ */

@Composable
fun PixelSkyDecor(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val u = size.height / 100f
        fun px(x: Float, y: Float, w: Float, h: Float, c: Color) =
            drawRect(c, Offset(x * u, y * u), Size(w * u, h * u))

        val cloud = CozyAuth.Sky
        val cloudEdge = Color(0xFFE7DCC2)
        val star = CozyAuth.MutedYellow
        val spark = Color(0xFFD9B96A)

        // a couple of small clouds (relative to a 0..(w/u) x 0..100 grid)
        val gw = size.width / u
        fun cloud(cx: Float, cy: Float, s: Float) {
            px(cx, cy, 14 * s, 5 * s, cloud)
            px(cx + 3 * s, cy - 3 * s, 8 * s, 4 * s, cloud)
            px(cx, cy + 5 * s, 14 * s, 1.5f * s, cloudEdge)
        }
        cloud(gw * 0.10f, 16f, 0.7f)
        cloud(gw * 0.66f, 10f, 0.9f)

        fun starAt(cx: Float, cy: Float, s: Float, c: Color) {
            px(cx - s, cy, 3 * s, s, c)   // horizontal
            px(cx, cy - s, s, 3 * s, c)   // vertical
        }
        starAt(gw * 0.30f, 30f, 2.2f, star)
        starAt(gw * 0.82f, 34f, 1.6f, spark)
        starAt(gw * 0.50f, 12f, 1.4f, spark)
        starAt(gw * 0.20f, 48f, 1.4f, spark)
        starAt(gw * 0.90f, 60f, 1.8f, star)
    }
}

/* ------------------------------------------------------------------ */
/* Mascot walking across the grass line (8-frame cycle).               */
/* ------------------------------------------------------------------ */

private val mascotFrames = intArrayOf(
    R.drawable.mascot_walk_01,
    R.drawable.mascot_walk_02,
    R.drawable.mascot_walk_03,
    R.drawable.mascot_walk_04,
    R.drawable.mascot_walk_05,
    R.drawable.mascot_walk_06,
    R.drawable.mascot_walk_07,
    R.drawable.mascot_walk_08
)

private const val MASCOT_ASPECT = 230f / 485f
private const val FRAME_MS = 110L
private const val CROSS_MS = 9_000L

/**
 * The HomeNotes mascot strolls back and forth along the grass. [mascotHeight] is its
 * on-screen height; [bottomPadding] lifts the feet to sit on the grass blades.
 */
@Composable
fun AuthMascotWalk(
    mascotHeight: Int = 96,
    bottomPadding: Int = 18,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberAnimationsEnabled()
    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { now -> elapsed = now - start }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth
        val h = mascotHeight.dp
        val mascotW = h * MASCOT_ASPECT

        // ping-pong progress 0..1..0 over a full there-and-back cycle
        val period = CROSS_MS * 2
        val t = (elapsed % period).toFloat() / CROSS_MS
        val goingRight = t <= 1f
        val p = if (goingRight) t else 2f - t

        val startX = -mascotW
        val endX = w
        val x = startX + (endX - startX) * p
        val frameIndex = ((elapsed / FRAME_MS) % mascotFrames.size).toInt()
        val frameId = if (animationsEnabled) mascotFrames[frameIndex] else mascotFrames[0]

        Image(
            bitmap = pixelImageBitmap(frameId),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = x, y = (-bottomPadding).dp)
                .size(width = mascotW, height = h)
                .graphicsLayer {
                    // sprite faces right; flip when walking left
                    scaleX = if (goingRight) 1f else -1f
                    transformOrigin = TransformOrigin.Center
                }
        )
    }
}

/* ------------------------------------------------------------------ */
/* Bottom pixel scene: grass strip, plants, mailbox, house, mascot.    */
/* ------------------------------------------------------------------ */

@Composable
fun AuthBottomScene(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // grass + soil strip pinned to the very bottom, spanning full width
        Image(
            bitmap = pixelImageBitmap(R.drawable.auth_grass_strip),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
        )

        // fence (back-left)
        Image(
            bitmap = pixelImageBitmap(R.drawable.auth_fence),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 6.dp, y = (-40).dp)
                .height(40.dp)
        )

        // bush
        Image(
            bitmap = pixelImageBitmap(R.drawable.auth_bush),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 96.dp, y = (-34).dp)
                .height(38.dp)
        )

        // pink flower
        Image(
            bitmap = pixelImageBitmap(R.drawable.auth_flower_pink),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 64.dp, y = (-30).dp)
                .height(30.dp)
        )

        // white flower (centre-ish)
        Image(
            bitmap = pixelImageBitmap(R.drawable.auth_flower_white),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 24.dp, y = (-26).dp)
                .height(24.dp)
        )


        // mailbox (front-right)
        Image(
            bitmap = pixelImageBitmap(R.drawable.auth_mailbox),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-14).dp, y = (-30).dp)
                .height(56.dp)
        )

        // mascot walks in front of everything
        AuthMascotWalk(
            mascotHeight = 92,
            bottomPadding = 16,
            modifier = Modifier.fillMaxSize()
        )
    }
}
