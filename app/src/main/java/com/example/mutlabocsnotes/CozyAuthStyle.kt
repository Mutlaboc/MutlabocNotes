package com.example.mutlabocsnotes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Warm, cozy pixel-art palette and reusable building blocks for the HomeNotes
 * registration screen. Colours follow the written brief: cream / beige background,
 * warm brown outlines, terracotta primary accent, soft green and muted yellow decor.
 */
object CozyAuth {
    val Cream = Color(0xFFF4ECD8)
    val CardCream = Color(0xFFFBF5E6)
    val FieldCream = Color(0xFFFFFDF6)

    val Ink = Color(0xFF5A3E25)          // primary text / dark pixel outline
    val InkSoft = Color(0xFF7A5A3A)      // secondary text
    val Hint = Color(0xFFAA936F)         // placeholder / hint

    val BrownOutline = Color(0xFF6E4A2A)
    val BrownShadow = Color(0xFFCBB58C)  // subtle pixel shadow under card

    val Terracotta = Color(0xFFC96E3E)
    val TerracottaDark = Color(0xFF7A3F1E)
    val TerracottaShadow = Color(0xFF9E5125)

    val InputBorder = Color(0xFFCBB489)
    val Divider = Color(0xFFD8C5A0)

    val Sky = Color(0xFFFFFFFF)
    val MutedYellow = Color(0xFFE8C45A)
    val SoftGreen = Color(0xFF7FB04A)

    /** Embedded Terminus-derived pixel font with full Cyrillic (OFL). */
    val PixelFont = FontFamily(Font(R.font.terminus_pixel))
}

fun Modifier.pixelScreenFrame(): Modifier = drawBehind {
    val outer = 4.dp.toPx()
    val inner = 8.dp.toPx()
    drawRect(CozyAuth.BrownOutline.copy(alpha = 0.32f), size = Size(size.width, outer))
    drawRect(
        CozyAuth.BrownOutline.copy(alpha = 0.32f),
        topLeft = Offset(0f, size.height - outer),
        size = Size(size.width, outer)
    )
    drawRect(CozyAuth.BrownOutline.copy(alpha = 0.32f), size = Size(outer, size.height))
    drawRect(
        CozyAuth.BrownOutline.copy(alpha = 0.32f),
        topLeft = Offset(size.width - outer, 0f),
        size = Size(outer, size.height)
    )
    drawRect(
        CozyAuth.MutedYellow.copy(alpha = 0.30f),
        topLeft = Offset(inner, inner),
        size = Size(outer, outer)
    )
    drawRect(
        CozyAuth.MutedYellow.copy(alpha = 0.30f),
        topLeft = Offset(size.width - inner - outer, inner),
        size = Size(outer, outer)
    )
    drawRect(
        CozyAuth.MutedYellow.copy(alpha = 0.30f),
        topLeft = Offset(inner, size.height - inner - outer),
        size = Size(outer, outer)
    )
    drawRect(
        CozyAuth.MutedYellow.copy(alpha = 0.30f),
        topLeft = Offset(size.width - inner - outer, size.height - inner - outer),
        size = Size(outer, outer)
    )
}

/**
 * Chunky 2-tone pixel frame with an offset pixel shadow. Squared corners keep the
 * nearest-neighbour pixel feel; [cornerRadius] adds the light "pixel edging".
 */
@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    fill: Color = CozyAuth.CardCream,
    border: Color = CozyAuth.BrownOutline,
    shadow: Color = CozyAuth.BrownShadow,
    borderWidth: Int = 3,
    shadowOffset: Int = 6,
    cornerRadius: Int = 6,
    content: @Composable () -> Unit
) {
    val shape = if (cornerRadius <= 0) RectangleShape else RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier
            .drawBehind {
                val o = shadowOffset.dp.toPx()
                drawRect(color = shadow, topLeft = Offset(o, o), size = size)
            }
            .clip(shape)
            .background(fill)
            .border(borderWidth.dp, border, shape)
    ) {
        content()
    }
}

/**
 * Primary terracotta pixel button: dark outline + lower shadow lip. Implemented as a
 * styled clickable panel so the look stays consistent with the pixel UI.
 */
@Composable
fun PixelPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val face = if (enabled) CozyAuth.Terracotta else CozyAuth.Terracotta.copy(alpha = 0.5f)
    PixelClickable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        fill = face,
        border = CozyAuth.TerracottaDark,
        shadow = CozyAuth.TerracottaShadow
    ) {
        Text(
            text = text,
            color = Color(0xFFFFF6EC),
            fontFamily = CozyAuth.PixelFont,
            fontSize = 17.sp
        )
    }
}

/** Light, secondary social-sign-in pixel button (less dominant than the primary). */
@Composable
fun PixelSocialButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: @Composable () -> Unit = {}
) {
    PixelClickable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        fill = CozyAuth.FieldCream,
        border = CozyAuth.InputBorder,
        shadow = CozyAuth.BrownShadow,
        shadowOffset = 4
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading()
            Text(
                text = text,
                color = CozyAuth.Ink,
                fontFamily = CozyAuth.PixelFont,
                fontSize = 15.sp
            )
        }
    }
}

/** Shared clickable pixel panel with centered content and an offset shadow lip. */
@Composable
fun PixelClickable(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    fill: Color,
    border: Color,
    shadow: Color,
    borderWidth: Int = 3,
    shadowOffset: Int = 5,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .drawBehind {
                val o = shadowOffset.dp.toPx()
                drawRect(color = shadow, topLeft = Offset(0f, o), size = size)
            }
            .clip(RoundedCornerShape(4.dp))
            .background(fill)
            .border(borderWidth.dp, border, RoundedCornerShape(4.dp))
            .then(if (enabled) Modifier else Modifier.semantics { disabled() })
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Thin divider line with centered text (e.g. "или"). */
@Composable
fun PixelDividerWithText(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .background(CozyAuth.Divider)
        )
        Text(
            text = text,
            color = CozyAuth.InkSoft,
            fontFamily = CozyAuth.PixelFont,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(
            Modifier
                .weight(1f)
                .height(2.dp)
                .background(CozyAuth.Divider)
        )
    }
}
