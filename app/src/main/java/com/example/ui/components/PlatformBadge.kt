package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MediaPlatform

fun getPlatformBrandColor(platform: MediaPlatform): Color {
    return when (platform) {
        MediaPlatform.TIKTOK -> Color(0xFF00F2FE)
        MediaPlatform.YOUTUBE -> Color(0xFFFF0033)
        MediaPlatform.INSTAGRAM -> Color(0xFFE1306C)
        MediaPlatform.TWITTER -> Color(0xFF1DA1F2)
        MediaPlatform.FACEBOOK -> Color(0xFF1877F2)
        MediaPlatform.PINTEREST -> Color(0xFFE60023)
        MediaPlatform.THREADS -> Color(0xFFFFFFFF)
        MediaPlatform.DOUYIN -> Color(0xFFFE2C55)
        MediaPlatform.REDNOTE -> Color(0xFFFF2442)
        MediaPlatform.BILIBILI -> Color(0xFF00AEEC)
        MediaPlatform.PIXIV -> Color(0xFF0096FA)
        MediaPlatform.SPOTIFY -> Color(0xFF1DB954)
        MediaPlatform.APPLE_MUSIC -> Color(0xFFFA243C)
        MediaPlatform.BANDCAMP -> Color(0xFF629AA9)
        MediaPlatform.UNKNOWN -> Color(0xFF888888)
    }
}

@Composable
fun PlatformBadge(
    platform: MediaPlatform,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val brandColor = getPlatformBrandColor(platform)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) brandColor else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .background(
                if (isSelected) brandColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(brandColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = platform.displayName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
