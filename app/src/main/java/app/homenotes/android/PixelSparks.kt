package app.homenotes.android

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Пиксельные «брызги»: [count] квадратиков разлетаются из центра узла, замедляясь и
 * затухая, пока [progress] идёт 0..1. Общий словарь для мелких откликов интерфейса —
 * искры под монетой в карточке и всплеск под кнопкой нижней панели.
 *
 * Размеры и разлёт заданы долями меньшей стороны узла, поэтому эффект одинаково
 * выглядит на холсте любого размера. [gravity] опускает квадратики к концу полёта,
 * [angleOffset] проворачивает кольцо (удобно, когда колец несколько).
 */
internal fun DrawScope.drawPixelSparks(
    progress: Float,
    colors: List<Color>,
    count: Int = 6,
    reachStart: Float = 0.45f,
    reachGrowth: Float = 0.75f,
    sparkFactor: Float = 0.14f,
    gravity: Float = 0f,
    angleOffset: Float = 0f
) {
    if (progress <= 0f || progress >= 1f || colors.isEmpty() || count <= 0) return
    val ease = 1f - (1f - progress) * (1f - progress)
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val reach = size.minDimension * (reachStart + reachGrowth * ease)
    val spark = size.minDimension * sparkFactor
    val drop = size.minDimension * gravity * ease * ease
    repeat(count) { i ->
        val angle = (i.toFloat() / count) * 2f * PI.toFloat() - PI.toFloat() / 2f + angleOffset
        drawRect(
            color = colors[i % colors.size],
            topLeft = Offset(
                center.x + cos(angle) * reach - spark / 2f,
                center.y + sin(angle) * reach - spark / 2f + drop
            ),
            size = Size(spark, spark),
            alpha = alpha
        )
    }
}
