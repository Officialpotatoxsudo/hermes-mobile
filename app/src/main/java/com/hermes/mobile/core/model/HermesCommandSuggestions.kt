package com.hermes.mobile.core.model

data class HermesCommandSuggestion(
    val command: String,
    val description: String,
    val hint: String,
    val categoryId: String = "",
)

fun hermesCommandSuggestions(
    selectedModel: String,
    selectedLabel: String,
    query: String = "",
): List<HermesCommandSuggestion> {
    val catalogSuggestions = hermesFeatureCatalog.flatMap { category ->
        category.actions
            .filter { it.kind == HermesFeatureActionKind.Command }
            .map { action ->
                HermesCommandSuggestion(
                    command = action.target.trim(),
                    description = "${category.title}: ${action.title}",
                    hint = action.subtitle,
                    categoryId = category.id,
                )
            }
    }
    val cleanSelectedModel = selectedModel.cleanSuggestionLine().orEmpty()
    val selectedModelSuggestion = cleanSelectedModel.takeIf { it.isNotBlank() }?.let {
        HermesCommandSuggestion(
            command = "/model $it",
            description = "Models: Use ${selectedLabel.cleanSuggestionLine() ?: it}",
            hint = "Switch model for this conversation",
        )
    }
    val suggestions = (catalogSuggestions + listOfNotNull(selectedModelSuggestion)).distinctBy { it.command }
    val cleanQuery = query.cleanSuggestionLine().orEmpty()
    if (cleanQuery.isBlank() || cleanQuery == "/") return suggestions

    val token = cleanQuery.removePrefix("/")
    val filtered = suggestions.filter { suggestion ->
        suggestion.command.startsWith(cleanQuery, ignoreCase = true) || (!cleanQuery.startsWith("/") && (
            suggestion.description.contains(cleanQuery.removePrefix("/"), ignoreCase = true) ||
                suggestion.hint.contains(token, ignoreCase = true)
            ))
    }
    return filtered.ifEmpty {
        val fallbackCommand = cleanQuery.withSlashPrefix()
        listOf(
            HermesCommandSuggestion(
                command = fallbackCommand,
                description = "Run $fallbackCommand",
                hint = "Send custom Hermes command",
            ),
        )
    }
}

private fun String.withSlashPrefix(): String {
    return if (startsWith("/")) this else "/$this"
}

private fun String.cleanSuggestionLine(): String? {
    return lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
}
