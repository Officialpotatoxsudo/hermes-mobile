package com.hermes.mobile.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatMediaPicker(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onPhotos: () -> Unit,
    onFiles: () -> Unit = {},
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                RoundedCornerShape(20.dp),
            ),
    ) {
        DropdownMenuItem(
            text = { Text("Camera") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.CameraAlt,
                    contentDescription = "Take photo",
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            onClick = {
                onCamera()
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text("Photos") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.PhotoLibrary,
                    contentDescription = "Pick from gallery",
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            onClick = {
                onPhotos()
                onDismiss()
            },
        )
        DropdownMenuItem(
            text = { Text("Files") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.AttachFile,
                    contentDescription = "Attach file",
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            onClick = {
                onFiles()
                onDismiss()
            },
        )
    }
}
