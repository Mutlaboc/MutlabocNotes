package com.example.homenotes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** One burst of coins flying from a note to the header coin counter. */
data class CoinFlight(
    val id: Long,
    val noteId: String,
    val startRoot: Offset,   // source centre, in root/window coordinates (px)
    val count: Int
)

/** Shards thrown off when a note "explodes". */
data class NoteBurst(
    val id: Long,
    val centerRoot: Offset,  // card centre, root/window coordinates (px)
    val color: Color
)

private const val FLIGHT_MS = 650
private const val MAX_FLY_COINS = 8
private const val COIN_DP = 22f

private const val PARTICLE_MS = 520
private const val PARTICLE_COUNT = 18
private const val GRAVITY_PX = 260f

/**
 * Full-screen overlay that draws the burst shards and the in-flight coins. Endpoints are
 * given in root coordinates and converted to this overlay's local space. When a flight
 * finishes, [onFlightArrived] fires so the caller can commit the note completion (which
 * bumps the real total); when a shard burst finishes, [onBurstFinished] clears it.
 *
 * Per-frame motion is read inside offset {} / graphicsLayer {} lambdas, so ticking the
 * animations never recomposes — it only re-lays-out / re-draws the sprites.
 */
@Composable
fun CoinFlightOverlay(
    flights: List<CoinFlight>,
    bursts: List<NoteBurst>,
    targetRoot: Offset,
    onFlightArrived: (CoinFlight) -> Unit,
    onBurstFinished: (NoteBurst) -> Unit,
    modifier: Modifier = Modifier
) {
    var originRoot by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { originRoot = it.positionInRoot() }
    ) {
        bursts.forEach { burst ->
            key(burst.id) {
                BurstParticles(
                    burst = burst,
                    center = burst.centerRoot - originRoot,
                    onFinished = { onBurstFinished(burst) }
                )
            }
        }
        flights.forEach { flight ->
            key(flight.id) {
                FlyingCoins(
                    flight = flight,
                    start = flight.startRoot - originRoot,
                    end = targetRoot - originRoot,
                    onArrived = { onFlightArrived(flight) }
                )
            }
        }
    }
}

@Composable
private fun BurstParticles(
    burst: NoteBurst,
    center: Offset,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(burst.id) {
        progress.animateTo(1f, animationSpec = tween(PARTICLE_MS, easing = LinearOutSlowInEasing))
        onFinished()
    }

    val n = PARTICLE_COUNT
    repeat(n) { i ->
        val rnd = rng(burst.id, i)
        val angle = (i.toFloat() / n) * 2f * PI.toFloat() + (rnd(0) - 0.5f) * 0.7f
        val reach = 70f + rnd(1) * 190f
        val sizeDp = 6f + rnd(2) * 8f
        val spin = (rnd(3) - 0.5f) * 900f
        val color = when (i % 3) {
            0 -> burst.color
            1 -> burst.color.darken(0.78f)
            else -> burst.color.darken(0.6f)
        }
        val cosA = cos(angle)
        val sinA = sin(angle)

        Box(
            modifier = Modifier
                .offset {
                    val p = progress.value
                    val ease = 1f - (1f - p) * (1f - p)          // decelerate outward
                    val dx = cosA * reach * ease
                    val dy = sinA * reach * ease + GRAVITY_PX * p * p
                    val half = (sizeDp / 2f).dp.toPx()
                    IntOffset((center.x + dx - half).roundToInt(), (center.y + dy - half).roundToInt())
                }
                .size(sizeDp.dp)
                .graphicsLayer {
                    val p = progress.value
                    rotationZ = spin * p
                    alpha = (1f - p * p).coerceIn(0f, 1f)
                }
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun FlyingCoins(
    flight: CoinFlight,
    start: Offset,
    end: Offset,
    onArrived: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(flight.id) {
        progress.animateTo(1f, animationSpec = tween(FLIGHT_MS, easing = FastOutSlowInEasing))
        onArrived()
    }

    val coin = painterResource(id = R.drawable.gold_coin)
    val n = flight.count.coerceIn(1, MAX_FLY_COINS)
    val arcHeight = (-(140f + flight.count * 8f))   // higher arc for richer notes

    repeat(n) { i ->
        // Each coin lags slightly behind the previous one for a trailing "stream".
        val delayFrac = 0.30f
        val phase = if (n > 1) i.toFloat() / (n - 1) else 0f
        val lateral = (i - (n - 1) / 2f) * 7f

        Image(
            painter = coin,
            contentDescription = null,
            modifier = Modifier
                .offset {
                    val raw = progress.value
                    val p = (((raw - delayFrac * phase) / (1f - delayFrac)).coerceIn(0f, 1f))
                    val x = start.x + (end.x - start.x) * p + lateral * (1f - p)
                    val arc = arcHeight * sin(PI * p).toFloat()
                    val y = start.y + (end.y - start.y) * p + arc
                    val half = (COIN_DP / 2f).dp.toPx()
                    IntOffset((x - half).roundToInt(), (y - half).roundToInt())
                }
                .size(COIN_DP.dp)
                .graphicsLayer {
                    val raw = progress.value
                    val p = (((raw - delayFrac * phase) / (1f - delayFrac)).coerceIn(0f, 1f))
                    val sc = 1f - 0.45f * p
                    scaleX = sc
                    scaleY = sc
                    alpha = if (p > 0.85f) (1f - (p - 0.85f) / 0.15f).coerceIn(0f, 1f) else 1f
                }
        )
    }
}

/** Deterministic per-particle pseudo-random: rng(seed, index)(k) -> [0,1). */
private fun rng(seed: Long, i: Int): (Int) -> Float = { k ->
    var h = (seed * 73856093L) xor (i.toLong() * 19349663L) xor (k.toLong() * 83492791L)
    h = h xor (h ushr 13)
    h *= -0x61c8864680b583ebL
    h = h xor (h ushr 16)
    ((h and 0xffffffL).toFloat() / 0x1000000L.toFloat())
}

private fun Color.darken(f: Float) = Color(red * f, green * f, blue * f, alpha)
