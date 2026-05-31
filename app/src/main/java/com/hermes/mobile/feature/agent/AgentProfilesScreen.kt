package com.hermes.mobile.feature.agent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.feature.sessions.SessionListViewModel

@Composable
fun AgentProfilesScreen(
    onBack: () -> Unit,
    onAgentOpen: (AgentProfile) -> Unit,
    viewModel: SessionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val defaultAgentId = AppPreferences.defaultAgents.first().id
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    fun resetForm() {
        editingId = null
        name = ""
        subtitle = ""
        instructions = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Agents",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Local chat profiles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = subtitle,
            onValueChange = { subtitle = it },
            label = { Text("Role") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            label = { Text("Instructions") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editingId != null || name.isNotBlank() || subtitle.isNotBlank()) {
                TextButton(onClick = ::resetForm) {
                    Text("Cancel")
                }
            }
            Button(
                enabled = name.trim().isNotBlank(),
                onClick = {
                    val id = editingId
                    if (id == null) {
                        viewModel.addAgent(name, subtitle.ifBlank { "Custom Hermes agent" }, instructions)
                    } else {
                        viewModel.updateAgent(id, name, subtitle.ifBlank { "Custom Hermes agent" }, instructions)
                    }
                    resetForm()
                },
            ) {
                Text(if (editingId == null) "Add" else "Save")
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 112.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.agents, key = { it.id }) { agent ->
                AgentProfileRow(
                    agent = agent,
                    isDefault = agent.id == defaultAgentId,
                    onOpen = { onAgentOpen(agent) },
                    onEdit = {
                        editingId = agent.id
                        name = agent.name
                        subtitle = agent.subtitle
                        instructions = agent.instructions
                    },
                    onDelete = {
                        if (agent.id != defaultAgentId) {
                            viewModel.removeAgent(agent.id)
                            if (editingId == agent.id) resetForm()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AgentProfileRow(
    agent: AgentProfile,
    isDefault: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AgentProfileAvatar(agent = agent, isDefault = isDefault)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
        ) {
            Text(agent.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                agent.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onOpen) {
            Icon(Icons.Rounded.ChatBubble, contentDescription = "Open ${agent.name}")
        }
        IconButton(enabled = !isDefault, onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit ${agent.name}")
        }
        IconButton(enabled = !isDefault, onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete ${agent.name}")
        }
    }
}

@Composable
private fun AgentProfileAvatar(agent: AgentProfile, isDefault: Boolean) {
    val avatarPlateColor = if (isDefault && isSystemInDarkTheme()) {
        Color.White.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(avatarPlateColor)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val avatarUri = agent.avatarUri?.takeIf { it.isNotBlank() }
        when {
            avatarUri != null -> {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "${agent.name} avatar",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            isDefault -> {
                Image(
                    painter = painterResource(id = com.hermes.mobile.R.drawable.hermes_mark),
                    contentDescription = "Default agent avatar",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            else -> {
                Text(agent.initial, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
