package com.example.cw.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val BgDeep = Color(0xFF02050A)
internal val BgMid = Color(0xFF071525)
internal val BgCard = Color(0xFF0D1E2E)
internal val BgCardAlt = Color(0xFF091622)
internal val BorderDim = Color(0xFF1A3550)
internal val BorderGlow = Color(0xFF2A6090)
internal val AccentCyan = Color(0xFF59D0FF)
internal val AccentGold = Color(0xFFF6CB7D)
internal val AccentGreen = Color(0xFF9BE7AE)
internal val AccentRed = Color(0xFFFF7868)
internal val NodeUpgradeIndicatorOutline = Color(0xFF102132)
internal val TextPrimary = Color(0xFFEAF4FF)
internal val TextSecond = Color(0xFF7A9EBB)
internal val TextDim = Color(0xFF3D6278)
internal val HudFundsChipBackground = Color(0xCC2D2110)
internal val HudFundsBadgeBackground = Color(0xFFFFDA62)
internal val HudFundsBadgeBorder = Color(0x66FFF0B0)
internal val HudFundsBadgeText = Color(0xFF4B3300)
internal val HudFundsLabelText = Color(0xFFE8C45A)
internal val HudFundsValueText = Color(0xFFFFE28A)
internal val HudOverlaySurface = Color(0xC0182735)
internal val LevelCardCompletedTop = Color(0xFF133247)
internal val LevelCardCompletedBottom = Color(0xFF0A1D2D)
internal val LevelCardUnlockedTop = Color(0xFF112A3C)
internal val LevelCardUnlockedBottom = Color(0xFF0B1C2B)
internal val LevelCardLockedTop = Color(0xFF0B1620)
internal val LevelCardLockedBottom = Color(0xFF07111A)

internal val SpaceBg = Brush.verticalGradient(listOf(BgDeep, BgMid, Color(0xFF0A2331)))

@Composable
internal fun MenuBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBg)
    ) {
        content()
    }
}

@Composable
internal fun GlowDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = BorderDim, thickness = 1.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(1.dp)
                .align(Alignment.Center)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, AccentCyan.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
internal fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentCyan,
            contentColor = Color(0xFF051015),
            disabledContainerColor = TextDim,
            disabledContentColor = TextSecond
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier.sizeIn(minHeight = 48.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
internal fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BorderGlow),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AccentCyan,
            disabledContentColor = TextDim
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
        modifier = modifier.sizeIn(minHeight = 48.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
internal fun StarRow(stars: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { index ->
            Text(
                text = if (index < stars) "★" else "☆",
                color = if (index < stars) AccentGold else TextDim,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
internal fun StatPill(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BgCardAlt)
            .border(1.dp, BorderDim, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecond, fontSize = 11.sp, letterSpacing = 0.5.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecond, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
