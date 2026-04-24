package com.example.cw.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

internal const val UPGRADE_NODE_BUTTON_FALLBACK_WIDTH_DP = 104
internal const val UPGRADE_NODE_BUTTON_FALLBACK_HEIGHT_DP = 40

internal data class InGameHudSummary(
    val fundsLabel: String
)

@Composable
internal fun InGameHud(
    state: MatchState,
    onOpenMenu: () -> Unit
) {
    val hudSummary = inGameHudSummary(state)

    Column(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FundsHudChip(
                content = hudSummary.fundsLabel,
                modifier = Modifier.weight(1f)
            )
            HudActionChip(
                icon = "\u23f8",
                contentDescription = "Pause mission",
                onTap = onOpenMenu
            )
        }

        if (state.message.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xC0182735)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = state.message,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FundsHudChip(content: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xB2162533)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$",
                color = AccentGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = content,
                color = AccentGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HudActionChip(icon: String, contentDescription: String, onTap: () -> Unit) {
    Card(
        onClick = onTap,
        colors = CardDefaults.cardColors(containerColor = Color(0xCCF6CB7D)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = Color(0xFF102132),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
internal fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB030C14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .background(BgCard, RoundedCornerShape(16.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PAUSED",
                color = AccentCyan,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace
            )
            GlowDivider()
            PrimaryButton("RESUME", onResume, Modifier.fillMaxWidth())
            GhostButton("RESTART LEVEL", onRestart, Modifier.fillMaxWidth())
            GhostButton("QUIT MISSION", onQuit, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun LevelEndOverlay(
    state: MatchState,
    onLevels: () -> Unit
) {
    val won = state.status == MatchStatus.PLAYER_WON

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB030C14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .background(BgCard, RoundedCornerShape(16.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                if (won) AccentCyan else AccentRed,
                                Color.Transparent
                            )
                        )
                    )
            )

            Text(
                text = if (won) "MISSION COMPLETE" else "MISSION FAILED",
                color = if (won) AccentCyan else AccentRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )

            GlowDivider()

            if (won) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        Text(
                            text = if (index < state.earnedStars) "★" else "☆",
                            color = if (index < state.earnedStars) AccentGold else TextDim,
                            fontSize = 36.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgCardAlt, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatRow("Time", formatCompletionTime(state.elapsedSeconds))
                    StatRow(
                        "2★ Target",
                        "<= ${formatCompletionTime(state.starThresholds.twoStarTimeSeconds.toFloat())}"
                    )
                    StatRow(
                        "3★ Target",
                        "<= ${formatCompletionTime(state.starThresholds.threeStarTimeSeconds.toFloat())}"
                    )
                }

                if (state.earnedUpgradePoint) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("●", color = AccentGold, fontSize = 10.sp)
                        Text("+1 Upgrade Point earned", color = AccentGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (state.improvedBestStars) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("●", color = AccentGreen, fontSize = 10.sp)
                        Text("New best record!", color = AccentGreen, fontSize = 13.sp)
                    }
                }
            }

            GlowDivider()

            PrimaryButton("BACK TO MISSIONS", onLevels, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun UpgradeNodeButton(
    state: MatchState,
    viewportSize: IntSize,
    onUpgrade: (Int) -> Unit
) {
    if (viewportSize == IntSize.Zero) return

    val base = selectedUpgradablePlayerBase(state) ?: return
    val canvasSize = Size(viewportSize.width.toFloat(), viewportSize.height.toFloat())
    val center = worldToScreen(base.position, canvasSize, state.worldBounds)
    val radius = base.radius * scale(canvasSize, state.worldBounds)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val cost = upgradeCost(base)
    val canAfford = state.playerMoney >= cost
    var measuredButtonSize by remember { mutableStateOf(IntSize.Zero) }
    val fallbackButtonSize = with(density) {
        IntSize(
            UPGRADE_NODE_BUTTON_FALLBACK_WIDTH_DP.dp.toPx().roundToInt(),
            UPGRADE_NODE_BUTTON_FALLBACK_HEIGHT_DP.dp.toPx().roundToInt()
        )
    }
    val buttonSize = if (measuredButtonSize == IntSize.Zero) fallbackButtonSize else measuredButtonSize
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val safeAreaInsets = with(density) {
        EdgeInsets(
            left = safeDrawingPadding.calculateLeftPadding(layoutDirection).toPx().roundToInt(),
            top = safeDrawingPadding.calculateTopPadding().toPx().roundToInt(),
            right = safeDrawingPadding.calculateRightPadding(layoutDirection).toPx().roundToInt(),
            bottom = safeDrawingPadding.calculateBottomPadding().toPx().roundToInt()
        )
    }
    val buttonOffset = upgradeNodeButtonOffset(
        center = center,
        radius = radius,
        viewportSize = viewportSize,
        buttonSize = buttonSize,
        baseMarginPx = with(density) { 8.dp.toPx().roundToInt() },
        horizontalGapPx = with(density) { 6.dp.toPx().roundToInt() },
        verticalGapPx = with(density) { 4.dp.toPx().roundToInt() },
        edgeInsets = safeAreaInsets
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .offset { buttonOffset }
                .zIndex(2f)
                .widthIn(min = 64.dp)
                .heightIn(min = 40.dp)
                .background(
                    color = if (canAfford) AccentGold else Color(0xFF6C5A2B),
                    shape = RoundedCornerShape(50)
                )
                .onSizeChanged { measuredButtonSize = it }
                .graphicsLayer { alpha = if (measuredButtonSize == IntSize.Zero) 0f else 1f }
                .clickable(enabled = measuredButtonSize != IntSize.Zero) { onUpgrade(base.id) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$", color = Color(0xFF102132), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(formatFunds(cost), color = Color(0xFF102132), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

internal data class EdgeInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
)

internal fun upgradeNodeButtonOffset(
    center: Offset,
    radius: Float,
    viewportSize: IntSize,
    buttonSize: IntSize,
    baseMarginPx: Int,
    horizontalGapPx: Int,
    verticalGapPx: Int,
    edgeInsets: EdgeInsets = EdgeInsets()
): IntOffset {
    val minX = (edgeInsets.left + baseMarginPx).toFloat()
    val minY = (edgeInsets.top + baseMarginPx).toFloat()
    val maxX = (viewportSize.width - buttonSize.width - edgeInsets.right - baseMarginPx).toFloat()
    val maxY = (viewportSize.height - buttonSize.height - edgeInsets.bottom - baseMarginPx).toFloat()
    val preferredX = center.x + radius + horizontalGapPx
    val preferredY = center.y - radius - verticalGapPx

    return IntOffset(
        x = preferredX.coerceIn(minX, maxX.coerceAtLeast(minX)).toInt(),
        y = preferredY.coerceIn(minY, maxY.coerceAtLeast(minY)).toInt()
    )
}

internal fun selectedUpgradablePlayerBase(state: MatchState): BaseState? {
    if (state.selectedBaseIds.size != 1) return null
    val selectedBaseId = state.selectedBaseIds.first()
    val base = state.bases.firstOrNull { it.id == selectedBaseId && it.owner == Owner.PLAYER } ?: return null
    return base.takeIf { it.capLevel < it.maxLevel }
}

internal fun inGameHudSummary(state: MatchState): InGameHudSummary {
    return InGameHudSummary(
        fundsLabel = formatFunds(state.playerMoney)
    )
}

internal fun formatCompletionTime(elapsedSeconds: Float): String {
    val totalSeconds = elapsedSeconds.toInt().coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

internal fun formatStars(stars: Int): String {
    val filled = stars.coerceIn(0, 3)
    return buildString(3) {
        repeat(filled) { append('★') }
        repeat(3 - filled) { append('☆') }
    }
}
