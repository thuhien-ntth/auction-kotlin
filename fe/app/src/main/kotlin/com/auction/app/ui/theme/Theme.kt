package com.auction.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = AuctionBlue40,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = AuctionBlue90,
    onPrimaryContainer = AuctionBlue10,
    secondary = AuctionGold40,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = AuctionGold90,
    onSecondaryContainer = AuctionGold10,
    tertiary = AuctionGreen40,
    error = AuctionRed40,
    background = NeutralGrey99,
    onBackground = NeutralGrey10,
    surface = NeutralGrey99,
    onSurface = NeutralGrey10,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE0E2EC),
    onSurfaceVariant = NeutralGreyVariant30
)

private val DarkColors = darkColorScheme(
    primary = AuctionBlue80,
    onPrimary = AuctionBlue20,
    primaryContainer = AuctionBlue20,
    onPrimaryContainer = AuctionBlue90,
    secondary = AuctionGold80,
    onSecondary = AuctionGold20,
    secondaryContainer = AuctionGold20,
    onSecondaryContainer = AuctionGold90,
    tertiary = AuctionGreen80,
    error = AuctionRed80,
    background = NeutralGrey10,
    onBackground = NeutralGrey90,
    surface = NeutralGrey10,
    onSurface = NeutralGrey90,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF43474E),
    onSurfaceVariant = NeutralGreyVariant80
)

@Composable
fun AuctionAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
