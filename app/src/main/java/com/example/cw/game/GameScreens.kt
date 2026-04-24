package com.example.cw.game

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cw.game.levels.LevelSummary
import com.example.cw.game.levels.isLevelUnlocked

@Composable
internal fun HomeScreen(
    campaign: CampaignState,
    onLevels: () -> Unit,
    onUpgrades: () -> Unit
) {
    MenuBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CELL WARS",
                color = AccentCyan,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "GALACTIC CAMPAIGN",
                color = TextSecond,
                fontSize = 13.sp,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(40.dp))
            GlowDivider()
            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgCard)
                    .border(1.dp, BorderDim, RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("★", color = AccentGold, fontSize = 20.sp)
                Text(
                    "${campaign.availableStars} Stars Ready",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(40.dp))

            PrimaryButton("CAMPAIGN", onLevels, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            GhostButton("UPGRADES", onUpgrades, Modifier.fillMaxWidth())

            Spacer(Modifier.height(48.dp))

            Text(
                text = "Capture. Expand. Dominate.",
                color = TextDim,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
internal fun LevelSelectScreen(
    campaign: CampaignState,
    levels: List<LevelSummary>,
    loadError: String?,
    onBack: () -> Unit,
    onPlayLevel: (Int) -> Unit
) {
    MenuBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "CAMPAIGN",
                        color = AccentCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("Select Mission", color = TextSecond, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("READY", color = TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text("★", color = AccentGold, fontSize = 16.sp)
                    Text("${campaign.availableStars}", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(12.dp))
                    GhostButton("BACK", onBack)
                }
            }

            GlowDivider(Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (loadError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2A0D0D))
                            .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Text(loadError, color = AccentRed, fontSize = 13.sp)
                    }
                }

                if (levels.isEmpty() && loadError == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgCard)
                            .border(1.dp, BorderDim, RoundedCornerShape(10.dp))
                            .padding(20.dp)
                    ) {
                        Text("No missions found in assets/levels.", color = TextSecond, fontSize = 13.sp)
                    }
                }

                levels.forEach { level ->
                    LevelCard(level, campaign, onPlayLevel)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LevelCard(
    level: LevelSummary,
    campaign: CampaignState,
    onPlay: (Int) -> Unit
) {
    val unlocked = isLevelUnlocked(level, campaign)
    val stars = campaign.starsForLevel(level.levelId)
    val completed = level.levelId in campaign.completedLevels

    val borderColor = when {
        completed -> AccentCyan.copy(alpha = 0.35f)
        unlocked -> BorderGlow
        else -> BorderDim
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (completed) AccentCyan else if (unlocked) BorderGlow else BorderDim,
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = level.name.uppercase(),
                        color = if (unlocked) TextPrimary else TextSecond,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = level.description,
                        color = TextSecond,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(12.dp))
                StarRow(stars)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    completed -> Text(
                        "COMPLETED",
                        color = AccentGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    unlocked -> Text(
                        "AVAILABLE",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )

                    else -> Text(
                        "LOCKED - Complete Mission ${level.unlockAfterLevelId}",
                        color = TextDim,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                PrimaryButton(
                    label = if (completed) "REPLAY" else "DEPLOY",
                    onClick = { onPlay(level.levelId) },
                    enabled = unlocked
                )
            }
        }
    }
}

@Composable
internal fun UpgradesScreen(
    campaign: CampaignState,
    onBack: () -> Unit,
    onUpgradeCashRate: () -> Unit
) {
    MenuBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "UPGRADES",
                        color = AccentCyan,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("Fleet Enhancements", color = TextSecond, fontSize = 12.sp, letterSpacing = 1.sp)
                }
                GhostButton("BACK", onBack)
            }

            GlowDivider(Modifier.padding(horizontal = 20.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatPill("READY", "${campaign.availableStars} ★", AccentGold)
                    StatPill("EARNED", "${campaign.totalStars} ★", AccentCyan)
                }

                Spacer(Modifier.height(4.dp))

                UpgradeCard(
                    title = "CASH FLOW",
                    subtitle = "Economy Upgrade",
                    description = "Increases the rate at which funds accumulate during a mission. Each level adds +25% income speed.",
                    currentLevel = campaign.cashRateLevel,
                    cost = 1,
                    canAfford = campaign.availableStars > 0,
                    accentColor = AccentGold,
                    onUpgrade = onUpgradeCashRate
                )

                UpgradePlaceholderCard("FLEET SPEED", "Movement Upgrade", "Increases ship travel velocity between nodes.")
                UpgradePlaceholderCard("ASSAULT PROTOCOL", "Combat Upgrade", "Boosts attack strength of Assault-type bases.")
                UpgradePlaceholderCard("RELAY NETWORK", "Logistics Upgrade", "Extends the range bonus of Relay-type bases.")

                Spacer(Modifier.height(8.dp))

                Text(
                    "Mission stars are now your upgrade currency. Spend them here and improve your best ratings to refill reserves.",
                    color = TextDim,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun UpgradeCard(
    title: String,
    subtitle: String,
    description: String,
    currentLevel: Int,
    cost: Int,
    canAfford: Boolean,
    accentColor: Color,
    onUpgrade: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(subtitle, color = TextSecond, fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgCardAlt)
                        .border(1.dp, BorderDim, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("LVL $currentLevel", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(description, color = TextSecond, fontSize = 13.sp, lineHeight = 18.sp)

            GlowDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("COST", color = TextDim, fontSize = 11.sp, letterSpacing = 1.sp)
                    Text(
                        "$cost ★",
                        color = if (canAfford) AccentGold else TextDim,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                PrimaryButton(
                    label = "UPGRADE",
                    onClick = onUpgrade,
                    enabled = canAfford
                )
            }
        }
    }
}

@Composable
private fun UpgradePlaceholderCard(title: String, subtitle: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCardAlt)
            .border(1.dp, BorderDim, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(title, color = TextDim, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(subtitle, color = TextDim, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgCard)
                        .border(1.dp, BorderDim, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("LOCKED", color = TextDim, fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }
            Text(description, color = TextDim, fontSize = 12.sp)
        }
    }
}
