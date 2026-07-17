package app.homenotes.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun NotesLoadingMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = CozyAuth.Terracotta)
    }
}

@Composable
internal fun NotesStatusMessage(
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = CozyAuth.InkSoft,
            fontFamily = CozyAuth.PixelFont,
            fontSize = 15.sp
        )
        if (actionText != null && onAction != null) {
            PixelPrimaryButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
