package app.homenotes.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ------------------------------------------------------------------ *
 * Экран достижений: карточки каталога с пипами ступеней (бронза..платина),
 * прогрессом к следующей ступени и суммой очков в шапке.
 * ------------------------------------------------------------------ */

/** Цвет пипа ступени. */
internal fun achievementTierColor(tier: AchievementTier): Color = when (tier) {
    AchievementTier.BRONZE -> Color(0xFFB5713F)
    AchievementTier.SILVER -> Color(0xFFA9B2BC)
    AchievementTier.GOLD -> Color(0xFFE8C45A)
    AchievementTier.PLATINUM -> Color(0xFF8FD0C6)
}

@Composable
fun AchievementsScreen(
    items: List<AchievementUi>,
    totalPoints: Int,
    onBack: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = CozyAuth.Cream,
        topBar = {
            CozyTopBar(title = stringResource(R.string.achievements_title), onBack = onBack)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(CozyAuth.Cream)
                .pixelScreenFrame()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { TotalPointsHeader(totalPoints) }
                items(items, key = { it.definition.id }) { item ->
                    AchievementCard(item)
                }
            }
        }
    }
}

@Composable
private fun TotalPointsHeader(totalPoints: Int) {
    PixelPanel(
        fill = CozyAuth.CardCream,
        shadowOffset = 4,
        cornerRadius = 6,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.achievement_points_total),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🏆", fontSize = 18.sp)
                Spacer(Modifier.size(6.dp))
                Text(
                    text = totalPoints.toString(),
                    fontFamily = CozyAuth.PixelFont,
                    color = CozyAuth.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementUi) {
    PixelPanel(
        fill = CozyAuth.CardCream,
        shadowOffset = 4,
        cornerRadius = 6,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.definition.icon, fontSize = 24.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(item.definition.titleRes),
                        fontFamily = CozyAuth.PixelFont,
                        color = CozyAuth.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(item.definition.descriptionRes, item.targetThreshold),
                        fontFamily = CozyAuth.PixelFont,
                        color = CozyAuth.InkSoft,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    TierPips(item.unlockedTiers)
                    if (item.earnedPoints > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.achievement_points_plus, item.earnedPoints),
                            fontFamily = CozyAuth.PixelFont,
                            color = CozyAuth.InkSoft,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            val completed = item.nextTier == null
            PixelBar(
                progress = if (completed) 1f else {
                    item.value.toFloat() / item.targetThreshold.toFloat()
                },
                fill = if (completed) {
                    achievementTierColor(AchievementTier.PLATINUM)
                } else {
                    CozyAuth.SoftGreen
                },
                height = 12.dp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (completed) {
                    stringResource(R.string.achievement_completed)
                } else {
                    stringResource(
                        R.string.achievement_progress_format,
                        item.value.coerceAtMost(item.targetThreshold),
                        item.targetThreshold,
                    )
                },
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.InkSoft,
                fontSize = 12.sp
            )
        }
    }
}

/** Четыре пипа ступеней: закрашены взятые, невзятые — контуром. */
@Composable
private fun TierPips(unlocked: Set<AchievementTier>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        AchievementTier.entries.forEach { tier ->
            val taken = tier in unlocked
            val shape = RoundedCornerShape(3.dp)
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(shape)
                    .background(
                        if (taken) achievementTierColor(tier) else CozyAuth.FieldCream
                    )
                    .border(2.dp, CozyAuth.BrownOutline, shape)
            )
        }
    }
}
