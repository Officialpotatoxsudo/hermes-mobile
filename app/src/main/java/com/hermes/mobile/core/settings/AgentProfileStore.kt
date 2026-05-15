package com.hermes.mobile.core.settings

internal fun buildCustomAgentProfile(
    id: String,
    name: String,
    subtitle: String,
): AgentProfile? {
    val cleanId = id.cleanAgentId() ?: return null
    val cleanName = name.cleanProfileTextLine()?.takeCleanly(40) ?: return null
    val cleanSubtitle = subtitle.cleanProfileTextLine() ?: "Custom Hermes agent"
    return AgentProfile(
        id = cleanId,
        name = cleanName,
        subtitle = cleanSubtitle.takeCleanly(96),
        initial = cleanName.agentInitial(),
    )
}

private fun String.agentInitial(): String {
    return firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "A"
}

internal fun mergeStoredAgentProfiles(stored: List<AgentProfile>): List<AgentProfile> {
    val defaultAgent = AppPreferences.defaultAgents.first()
    val customAgents = stored.mapNotNull { agent ->
        if (agent.id.cleanAgentId() == defaultAgent.id) {
            null
        } else {
            buildCustomAgentProfile(agent.id, agent.name, agent.subtitle)
        }
    }
    return (AppPreferences.defaultAgents + customAgents).distinctBy { it.id }
}

internal fun upsertCustomAgent(
    current: List<AgentProfile>,
    agent: AgentProfile,
): List<AgentProfile> {
    val cleanAgent = buildCustomAgentProfile(agent.id, agent.name, agent.subtitle) ?: return mergeStoredAgentProfiles(current)
    val defaultAgent = AppPreferences.defaultAgents.first()
    if (cleanAgent.id == defaultAgent.id) return mergeStoredAgentProfiles(current).filterNot { it.id == defaultAgent.id }
    val customAgents = mergeStoredAgentProfiles(current).filterNot { it.id == defaultAgent.id }
    if (customAgents.none { it.id == cleanAgent.id }) return customAgents + cleanAgent

    return customAgents.map { existing ->
        if (existing.id == cleanAgent.id) cleanAgent else existing
    }.distinctBy { it.id }
}

internal fun removeCustomAgent(
    current: List<AgentProfile>,
    agentId: String,
): List<AgentProfile> {
    val cleanAgentId = agentId.cleanAgentId()
    val defaultAgent = AppPreferences.defaultAgents.first()
    val customAgents = mergeStoredAgentProfiles(current).filterNot { it.id == defaultAgent.id }
    if (cleanAgentId == defaultAgent.id) return customAgents
    return customAgents.filterNot { it.id == cleanAgentId }
}

private fun String.cleanAgentId(): String? {
    return trim()
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
        .takeIf { it.isNotBlank() }
}

private fun String.cleanProfileTextLine(): String? {
    return trim()
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()
        .takeIf { it.isNotBlank() }
}

private fun String.takeCleanly(maxLength: Int): String {
    if (length <= maxLength) return this
    val head = take(maxLength).trimEnd()
    val boundary = head.lastIndexOf(' ').takeIf { it >= MIN_PROFILE_TEXT_WORD_BOUNDARY }
    return boundary?.let { head.take(it) } ?: head
}

private const val MIN_PROFILE_TEXT_WORD_BOUNDARY = 24
