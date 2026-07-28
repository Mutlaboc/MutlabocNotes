package app.homenotes.android

import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * True when system animations are enabled (ANIMATOR_DURATION_SCALE != 0). Scenes and
 * micro-interactions must render a valid static state when this returns false.
 */
@Composable
internal fun rememberAnimationsEnabled(): Boolean {
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

/** Decode a complete raster frame sequence once for the lifetime of the composition. */
@Composable
internal fun rememberPixelBmps(vararg drawableIds: Int): List<ImageBitmap> {
    val resources = LocalContext.current.resources
    return remember(resources, drawableIds.contentHashCode()) {
        drawableIds.map { BitmapFactory.decodeResource(resources, it).asImageBitmap() }
    }
}

/** Shared frame clock. Disabled animations reset to the valid static first-frame state. */
@Composable
internal fun rememberElapsedMillis(enabled: Boolean): State<Long> {
    val elapsed = remember { mutableStateOf(0L) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            elapsed.value = 0L
            return@LaunchedEffect
        }
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameMs -> elapsed.value = frameMs - start }
        }
    }
    return elapsed
}
