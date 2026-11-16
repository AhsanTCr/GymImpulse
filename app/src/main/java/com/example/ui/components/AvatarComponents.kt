package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.utils.AvatarUtils
import com.example.utils.ImageCompressionUtils

@Composable
fun MemberAvatar(
    fullName: String,
    avatarUri: String? = null,
    size: Dp = 48.dp,
    borderColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val initials = AvatarUtils.extractInitials(fullName)
    val fallbackBg = AvatarUtils.colorForName(fullName)
    val borderModifier = if (borderColor != null) {
        Modifier.border(2.dp, borderColor, CircleShape)
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    }

    val decodedBitmap = remember(avatarUri) {
        if (avatarUri != null && avatarUri.startsWith("data:image/")) {
            ImageCompressionUtils.decodeBase64DataUri(avatarUri)?.asImageBitmap()
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(borderModifier)
            .background(fallbackBg),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = stringResource(R.string.cd_member_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            !avatarUri.isNullOrBlank() -> {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = stringResource(R.string.cd_member_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                val fontSize = (size.value * 0.36f).sp
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}
