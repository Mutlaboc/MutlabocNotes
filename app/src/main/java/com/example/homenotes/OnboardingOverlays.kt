package com.example.homenotes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

const val ONBOARDING_WELCOME_TEST_TAG = "onboarding_welcome"
const val ONBOARDING_WELCOME_BUTTON_TEST_TAG = "onboarding_welcome_button"
const val ONBOARDING_COACH_TEST_TAG = "onboarding_coach"
const val ONBOARDING_COACH_BUTTON_TEST_TAG = "onboarding_coach_button"

/**
 * Приветственный экран при первом запуске. Закрывается только кнопкой, чтобы
 * пользователь точно прочитал ключевое описание приложения.
 */
@Composable
fun OnboardingWelcomeDialog(
    title: String,
    message: String,
    buttonText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        PixelPanel(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ONBOARDING_WELCOME_TEST_TAG)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material.Text(
                    text = title,
                    color = CozyAuth.Ink,
                    fontFamily = CozyAuth.PixelFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                androidx.compose.material.Text(
                    text = message,
                    color = CozyAuth.InkSoft,
                    fontFamily = CozyAuth.PixelFont,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(22.dp))
                PixelPrimaryButton(
                    text = buttonText,
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ONBOARDING_WELCOME_BUTTON_TEST_TAG)
                )
            }
        }
    }
}

/**
 * Одноразовая подсказка-коучмарк: затемняет верх экрана и показывает облачко с
 * подсказкой над подсвеченной иконкой нижней панели. Нижняя панель остаётся
 * яркой, чтобы подсветка нужной иконки была видна.
 *
 * @param anchorRect границы подсвеченной иконки в координатах корня (в пикселях).
 */
@Composable
fun OnboardingCoachOverlay(
    anchorRect: Rect,
    title: String,
    message: String,
    buttonText: String,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val marginPx = with(density) { 12.dp.toPx() }
    val gapPx = with(density) { 12.dp.toPx() }
    val scrimHeight = with(density) { anchorRect.top.toDp() }

    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Затемняем только область над нижней панелью, чтобы подсвеченная иконка
            // оставалась яркой и заметной. Клик по затемнению гасится, не закрывая подсказку.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(scrimHeight)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            )

            var calloutSize by remember { mutableStateOf(IntSize.Zero) }
            val ready = calloutSize != IntSize.Zero
            val xRaw = anchorRect.center.x - calloutSize.width / 2f
            val maxX = (screenWidthPx - calloutSize.width - marginPx).coerceAtLeast(marginPx)
            val x = xRaw.coerceIn(marginPx, maxX)
            val y = (anchorRect.top - calloutSize.height - gapPx).coerceAtLeast(marginPx)

            Box(
                modifier = Modifier
                    .width(280.dp)
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .onGloballyPositioned { calloutSize = it.size }
                    .alpha(if (ready) 1f else 0f)
                    .testTag(ONBOARDING_COACH_TEST_TAG)
            ) {
                PixelPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        androidx.compose.material.Text(
                            text = title,
                            color = CozyAuth.Ink,
                            fontFamily = CozyAuth.PixelFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material.Text(
                            text = message,
                            color = CozyAuth.InkSoft,
                            fontFamily = CozyAuth.PixelFont,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PixelPrimaryButton(
                            text = buttonText,
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(ONBOARDING_COACH_BUTTON_TEST_TAG)
                        )
                    }
                }
            }
        }
    }
}
