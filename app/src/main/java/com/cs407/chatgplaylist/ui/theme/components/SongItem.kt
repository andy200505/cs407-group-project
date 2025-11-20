package com.cs407.chatgplaylist.ui.theme.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cs407.chatgplaylist.R

@Composable
fun SongItem(title: String, artist: String, spotifyUrl: String? = null) {
    val context = LocalContext.current
    val hasLink = !spotifyUrl.isNullOrEmpty()
    val onOpen: (() -> Unit)? = if (hasLink) {
        {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl))
            context.startActivity(intent)
        }
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(if (hasLink) 1f else 0.6f)
            .let { base ->
                if (onOpen != null) base.clickable { onOpen() } else base
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = artist, style = MaterialTheme.typography.bodyMedium)
        }

        Image(
            painter = painterResource(id = R.drawable.spotify),
            contentDescription = "Spotify Logo",
            modifier = Modifier
                .size(24.dp)
                .padding(start = 8.dp)
                .alpha(if (hasLink) 1f else 0.3f)
                .let { base ->
                    if (onOpen != null) base.clickable { onOpen() } else base
                }
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(top = 8.dp),
        thickness = DividerDefaults.Thickness,
        color = DividerDefaults.color
    )
}
