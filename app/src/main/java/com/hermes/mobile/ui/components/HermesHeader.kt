package com.hermes.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hermes.mobile.core.settings.HermesGlassRole

@Composable
fun HermesHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingAction: String? = null,
    onTrailingAction: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailingAction != null && onTrailingAction != null) {
            val actionShape = RoundedCornerShape(20.dp)
            Text(
                trailingAction,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .hermesGlass(
                        shape = actionShape,
                        role = HermesGlassRole.Action,
                        normalContainerAlpha = 0.5f,
                        normalBorderAlpha = 0.1f,
                    )
                    .semantics { role = Role.Button }
                    .clickable(onClick = onTrailingAction)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
