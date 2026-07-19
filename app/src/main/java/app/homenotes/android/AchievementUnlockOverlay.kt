package app.homenotes.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * WoW-плашка «Достижение получено!» по центру экрана. Рендерится поверх всех
 * роутов (сиблинг NavHost в MainActivity). Очередь двигает БД: авто-скрытие или
 * тап помечают анлок notified, и поток unnotified подставляет следующий тост.
 */
@Composable
fun AchievementUnlockOverlay(
    toast: AchievementToastUi?,
    onDismissed: (AchievementToastUi) -> Unit,
    onTapped: (AchievementToastUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = toast != null,
        enter = scaleIn(initialScale = 0.7f) + fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        // AnimatedVisibility держит последний контент на время exit-анимации.
        val shown = toast ?: return@AnimatedVisibility
        LaunchedEffect(shown.definition.id, shown.tier) {
            delay(TOAST_DURATION_MS)
            onDismissed(shown)
        }
        AchievementToastPlaque(
            toast = shown,
            onClick = { onTapped(shown) },
        )
    }
}

@Composable
private fun AchievementToastPlaque(
    toast: AchievementToastUi,
    onClick: () -> Unit,
) {
    val tierColor = achievementTierColor(toast.tier)
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(min = 240.dp, max = 340.dp)
            .clip(shape)
            .background(CozyAuth.CardCream)
            .border(3.dp, tierColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.achievement_unlocked),
            fontFamily = CozyAuth.PixelFont,
            color = CozyAuth.InkSoft,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = toast.definition.icon, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(toast.definition.titleRes),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TierPip(color = tierColor)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(tierNameRes(toast.tier)),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.InkSoft,
                fontSize = 13.sp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.achievement_points_plus, toast.points),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.MutedYellow,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun TierPip(color: Color) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(shape)
            .background(color)
            .border(2.dp, CozyAuth.BrownOutline, shape)
    )
}

internal fun tierNameRes(tier: AchievementTier): Int = when (tier) {
    AchievementTier.BRONZE -> R.string.achievement_tier_bronze
    AchievementTier.SILVER -> R.string.achievement_tier_silver
    AchievementTier.GOLD -> R.string.achievement_tier_gold
    AchievementTier.PLATINUM -> R.string.achievement_tier_platinum
}

private const val TOAST_DURATION_MS = 4_000L
