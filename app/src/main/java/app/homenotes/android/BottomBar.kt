package app.homenotes.android

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private data class BottomBarItem(
    val action: BottomBarAction,
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun BottomBar(
    selectedAction: BottomBarAction?,
    onCompletedNotesClick: () -> Unit,
    onAddClick: () -> Unit,
    onHomeInfoClick: () -> Unit,
    onNavigateHome: () -> Unit,
    highlightedAction: BottomBarAction? = null,
    onIconBounds: (BottomBarAction, Rect) -> Unit = { _, _ -> },
    onUpcomingClick: (() -> Unit)? = null,
) {
    val completedItem = BottomBarItem(
        action = BottomBarAction.CompletedNotes,
        text = stringResource(R.string.bottom_bar_completed),
        icon = Icons.Default.DoneAll,
        onClick = onCompletedNotesClick
    )
    val addItem = BottomBarItem(
        action = BottomBarAction.AddNote,
        text = stringResource(R.string.bottom_bar_create),
        icon = Icons.Default.Add,
        onClick = onAddClick
    )
    val homeInfoItem = BottomBarItem(
        action = BottomBarAction.HomeInfo,
        text = stringResource(R.string.bottom_bar_home_info),
        icon = Icons.Default.QuestionMark,
        onClick = onHomeInfoClick
    )
    val notesItem = BottomBarItem(
        action = BottomBarAction.Notes,
        text = stringResource(R.string.bottom_bar_notes),
        icon = Icons.Default.Home,
        onClick = onNavigateHome
    )
    val items = when (selectedAction) {
        BottomBarAction.CompletedNotes -> listOf(
            notesItem,
            addItem,
            BottomBarItem(
                action = BottomBarAction.UpcomingTasks,
                text = stringResource(R.string.bottom_bar_upcoming),
                icon = Icons.Default.Schedule,
                onClick = { onUpcomingClick?.invoke() }
            )
        )
        BottomBarAction.UpcomingTasks -> listOf(completedItem, addItem, notesItem)
        else -> listOf(completedItem, addItem, homeInfoItem)
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(CozyAuth.BrownOutline.copy(alpha = 0.35f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(CozyAuth.CardCream)
                .testTag(BOTTOM_BAR_TEST_TAG),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isHighlighted = item.action == highlightedAction
                val interactionSource = remember(item.action) { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = interactionSource,
                            // Для акцентной кнопки риппл на всю треть панели некрасив —
                            // вместо него кнопка сама реагирует на нажатие (see BottomBarButton).
                            indication = LocalIndication.current
                        ) { item.onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    BottomBarButton(
                        isHighlighted = isHighlighted,
                        interactionSource = interactionSource,
                        onPositioned = { rect -> onIconBounds(item.action, rect) },
                        text = item.text,
                        icon = item.icon
                    )
                }
            }
        }
    }
}

/**
 * Кнопка нижней панели: приподнятая на 8 dp пиксельная панель
 * 60 x 48 dp (MutedYellow, иконка — Terracotta) с подписью. Позади —
 * два прямоугольных слоя "света" шириной 72 и 84 dp, как ступенчатое свечение.
 */
@Composable
private fun BottomBarButton(
    isHighlighted: Boolean,
    interactionSource: InteractionSource,
    onPositioned: (Rect) -> Unit,
    text: String,
    icon: ImageVector
) {
    // Тактильный отклик вместо риппла: пока палец на кнопке, панель слегка
    // «вдавливается» (уменьшается и опускается), отпускание пружинит обратно.
    val pressed by interactionSource.collectIsPressedAsState()
    val animationsEnabled = rememberAnimationsEnabled()
    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (animationsEnabled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            snap()
        },
        label = "bottom_bar_press"
    )
    Column(
        // unbounded: колонка выше 56-dp панели (свечение + подпись) и приподнята
        // на 8 dp — ей разрешено выходить за границы строки без обрезки контента.
        modifier = Modifier
            .wrapContentHeight(unbounded = true)
            .offset(y = (-8).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Слои света: шире кнопки, от прозрачного к более плотному.
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 60.dp)
                    .background(
                        CozyAuth.MutedYellow.copy(alpha = 0.18f),
                        RoundedCornerShape(8.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 54.dp)
                    .background(
                        CozyAuth.MutedYellow.copy(alpha = 0.35f),
                        RoundedCornerShape(7.dp)
                    )
            )
            if (isHighlighted) {
                HighlightPulse(size = 76.dp)
            }
            PixelPanel(
                fill = CozyAuth.MutedYellow,
                shadowOffset = 3,
                cornerRadius = 6,
                modifier = Modifier
                    .size(width = 60.dp, height = 48.dp)
                    .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
                    // press читается только в graphicsLayer — анимация нажатия
                    // не рекомпозирует панель.
                    .graphicsLayer {
                        val s = 1f - 0.07f * press
                        scaleX = s
                        scaleY = s
                        translationY = 2.dp.toPx() * press
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        tint = CozyAuth.Terracotta
                    )
                }
            }
        }
        Text(
            text = text,
            fontFamily = CozyAuth.PixelFont,
            style = MaterialTheme.typography.caption,
            color = CozyAuth.InkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Пульсирующий кружок-подсветка под иконкой во время обучения. */
@Composable
private fun HighlightPulse(size: Dp = 40.dp) {
    val transition = rememberInfiniteTransition(label = "hint_pulse")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_alpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(CozyAuth.MutedYellow)
    )
}
