package com.hermes.mobile.feature.agent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.mobile.core.model.HermesFeatureAction
import com.hermes.mobile.core.model.HermesFeatureActionKind
import com.hermes.mobile.core.model.HermesFeatureCategory
import com.hermes.mobile.ui.components.frostedGlass
import kotlinx.coroutines.launch

@Composable
fun AgentControlScreen(
    onBack: () -> Unit,
    viewModel: AgentControlViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AgentNavigationDrawer(
                categories = state.categories,
                selected = state.selectedCategory,
                onSelect = { category ->
                    viewModel.selectCategory(category)
                    scope.launch { drawerState.close() }
                },
                onBack = {
                    scope.launch { drawerState.close() }
                    onBack()
                },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                )
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Header(
                    selectedCategory = state.selectedCategory,
                    onBack = onBack,
                    onMenu = { scope.launch { drawerState.open() } },
                )
            }
            item {
                CategoryRail(
                    categories = state.categories,
                    selected = state.selectedCategory,
                    onSelect = viewModel::selectCategory,
                )
            }
            item {
                Text(
                    state.selectedCategory.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            items(state.selectedCategory.actions, key = { it.id }) { action ->
                FeatureActionRow(
                    action = action,
                    selected = action.id == state.selectedAction.id,
                    onClick = { viewModel.selectAction(action) },
                )
            }
            item {
                ActionPanel(
                    action = state.selectedAction,
                    body = state.body,
                    response = state.response,
                    error = state.error,
                    isLoading = state.isLoading,
                    onBodyChanged = viewModel::onBodyChanged,
                    onRun = viewModel::runSelectedAction,
                )
            }
        }
    }
}

@Composable
private fun Header(selectedCategory: HermesFeatureCategory, onBack: () -> Unit, onMenu: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onMenu,
            modifier = Modifier
                .size(46.dp)
                .frostedGlass(
                    colors = MaterialTheme.colorScheme,
                    shape = CircleShape,
                    containerAlpha = 0.72f,
                    borderAlpha = 0.18f,
                ),
        ) {
            Icon(Icons.Rounded.Menu, contentDescription = "Agent menu")
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Agent", style = MaterialTheme.typography.headlineMedium)
            Text(
                selectedCategory.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChipButton(text = "Back", onClick = onBack)
    }
}

@Composable
private fun AgentNavigationDrawer(
    categories: List<HermesFeatureCategory>,
    selected: HermesFeatureCategory,
    onSelect: (HermesFeatureCategory) -> Unit,
    onBack: () -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier
            .widthIn(max = 330.dp)
            .fillMaxSize(),
        drawerContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 18.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
                    Text("Agent", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Native Hermes controls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(categories, key = { it.id }) { category ->
                AgentDrawerCategoryRow(
                    category = category,
                    selected = category.id == selected.id,
                    onClick = { onSelect(category) },
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
                DrawerBackRow(onBack)
            }
        }
    }
}

@Composable
private fun AgentDrawerCategoryRow(category: HermesFeatureCategory, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconPill(iconFor(category.actions.first().kind), selected)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(category.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                "${category.actions.size} real actions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DrawerBackRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text("Back to chats", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CategoryRail(
    categories: List<HermesFeatureCategory>,
    selected: HermesFeatureCategory,
    onSelect: (HermesFeatureCategory) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            ChipButton(
                text = category.title,
                selected = category.id == selected.id,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun FeatureActionRow(
    action: HermesFeatureAction,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (selected) 0.82f else 0.62f))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconPill(icon = iconFor(action.kind), selected = selected)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(action.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(action.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionPanel(
    action: HermesFeatureAction,
    body: String,
    response: String,
    error: String?,
    isLoading: Boolean,
    onBodyChanged: (String) -> Unit,
    onRun: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(
                colors = MaterialTheme.colorScheme,
                shape = RoundedCornerShape(28.dp),
                containerAlpha = 0.76f,
                borderAlpha = 0.18f,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(action.title, style = MaterialTheme.typography.titleMedium)
                Text(actionKindLabel(action), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ChipButton(
                text = when {
                    isLoading -> "Running"
                    action.kind == HermesFeatureActionKind.Read -> "Reload"
                    else -> "Run"
                },
                selected = true,
                onClick = onRun,
            )
        }
        Spacer(Modifier.height(14.dp))
        BasicTextField(
            value = body,
            onValueChange = onBodyChanged,
            enabled = !isLoading,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontFamily = if (body.trimStart().startsWith("{")) FontFamily.Monospace else FontFamily.Default,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 320.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.62f))
                .padding(14.dp),
            decorationBox = { inner ->
                if (body.isEmpty() && !isLoading) {
                    Text("No content loaded. Run action to fetch real server data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            },
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 10.dp))
        }
        AnimatedVisibility(visible = response.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
            Text(
                response,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        AnimatedVisibility(visible = shouldRenderMarkdown(action, body)) {
            MarkdownPreview(body)
        }
    }
}

@Composable
private fun MarkdownPreview(markdown: String) {
    Column(
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Rendered markdown", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        var inCode = false
        markdown.lineSequence().take(80).forEach { line ->
            when {
                line.startsWith("```") -> {
                    inCode = !inCode
                }
                inCode -> CodeLine(line)
                line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.titleLarge)
                line.startsWith("## ") -> Text(line.removePrefix("## "), style = MaterialTheme.typography.titleMedium)
                line.startsWith("- ") -> BulletLine(line.removePrefix("- "))
                line.isBlank() -> Spacer(Modifier.height(2.dp))
                else -> Text(line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row {
        Text("-", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CodeLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun ChipButton(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun IconPill(icon: ImageVector, selected: Boolean) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun iconFor(kind: HermesFeatureActionKind): ImageVector {
    return when (kind) {
        HermesFeatureActionKind.Read -> Icons.Rounded.Download
        HermesFeatureActionKind.Create -> Icons.Rounded.PlayArrow
        HermesFeatureActionKind.Update -> Icons.Rounded.Edit
        HermesFeatureActionKind.Delete -> Icons.Rounded.Delete
        HermesFeatureActionKind.Command -> Icons.Rounded.Terminal
    }
}

private fun actionKindLabel(action: HermesFeatureAction): String {
    return when (action.kind) {
        HermesFeatureActionKind.Read -> "Read from connected Hermes server"
        HermesFeatureActionKind.Create -> "Start native Hermes run"
        HermesFeatureActionKind.Update -> "Update connected Hermes server"
        HermesFeatureActionKind.Delete -> "Delete on connected Hermes server"
        HermesFeatureActionKind.Command -> "Run native Hermes action"
    }
}

private fun shouldRenderMarkdown(action: HermesFeatureAction, body: String): Boolean {
    return action.target.startsWith("agent/") ||
        body.contains("\n#") ||
        body.startsWith("#") ||
        body.contains("```") ||
        body.contains("\n- ")
}
