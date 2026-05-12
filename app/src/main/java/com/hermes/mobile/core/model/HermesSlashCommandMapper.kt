package com.hermes.mobile.core.model

sealed interface HermesSlashCommandAction {
    data object None : HermesSlashCommandAction
    data object NewSession : HermesSlashCommandAction
    data object Retry : HermesSlashCommandAction
    data object Undo : HermesSlashCommandAction
    data object Stop : HermesSlashCommandAction
    data object ClearDraft : HermesSlashCommandAction
    data class SelectModel(val requested: String) : HermesSlashCommandAction
    data class AgentPrompt(val prompt: String) : HermesSlashCommandAction
}

fun mapHermesSlashCommand(rawInput: String): HermesSlashCommandAction {
    val command = rawInput.trim()
    if (!command.startsWith("/")) return HermesSlashCommandAction.None

    val verb = command.substringBefore(" ").lowercase()
    return when (verb) {
        "/new", "/reset" -> HermesSlashCommandAction.NewSession
        "/retry" -> HermesSlashCommandAction.Retry
        "/undo" -> HermesSlashCommandAction.Undo
        "/stop" -> HermesSlashCommandAction.Stop
        "/clear" -> HermesSlashCommandAction.ClearDraft
        "/model" -> {
            val requested = command.removePrefix("/model").trim()
            if (requested.isBlank()) {
                HermesSlashCommandAction.AgentPrompt(command.toAgentPrompt())
            } else {
                HermesSlashCommandAction.SelectModel(requested)
            }
        }
        else -> HermesSlashCommandAction.AgentPrompt(command.toAgentPrompt())
    }
}

private fun String.toAgentPrompt(): String {
    return "Hermes native command requested from mobile: $this\n" +
        "Run the equivalent Hermes action if available, then return concise output."
}
