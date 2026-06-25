package com.example.mutlabocsnotes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource

@Composable
internal fun HomeHeader(
    totalCoins: Int,
    animationRestartKey: Any
) {
    val backgroundDescription = stringResource(R.string.home_background_description)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clipToBounds()
            .semantics { contentDescription = backgroundDescription },
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.home_meadow_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        HomeYardScene(
            animationRestartKey = animationRestartKey,
            modifier = Modifier
                .height(232.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colors.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.gold_coin),
                contentDescription = stringResource(R.string.total_coins_description),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = totalCoins.toString(),
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
internal fun UserMenu(
    userEmail: String,
    onSwitchUser: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.testTag(USER_MENU_BUTTON_TEST_TAG)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(R.string.user_description)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(onClick = {
                    expanded = false
                    onSwitchUser()
                }) {
                    Text(stringResource(R.string.switch_user))
                }
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onOpenSettings()
                    },
                    modifier = Modifier.testTag(USER_MENU_SETTINGS_ITEM_TEST_TAG)
                ) {
                    Text(stringResource(R.string.action_settings))
                }
                if (userEmail.isNotBlank()) {
                    DropdownMenuItem(onClick = { expanded = false }) {
                        Text(userEmail)
                    }
                }
            }
        }
    }
}
