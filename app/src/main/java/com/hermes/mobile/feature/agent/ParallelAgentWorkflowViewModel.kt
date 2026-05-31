package com.hermes.mobile.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.local.MessageEntity
import com.hermes.mobile.core.data.local.SessionEntity
import com.hermes.mobile.core.model.ChatCompletionRequest
import com.hermes.mobile.core.model.chatRequestMessage
import com.hermes.mobile.core.settings.AgentProfile
import com.hermes.mobile.core.settings.AppPreferences
import com.hermes.mobile.core.util.newAgentChatSessionId
import com.hermes.mobile.feature.chat.ChatStreamCommand
import com.hermes.mobile.feature.chat.ChatStreamCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParallelAgentWorkflowUiState(
    val agents: List<AgentProfile> = AppPreferences.defaultAgents,
    val selectedAgentIds: Set<String> = AppPreferences.defaultAgents.map { it.id }.toSet(),
    val prompt: String = "",
    val startedSessionIds: List<String> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class ParallelAgentWorkflowViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val streamCoordinator: ChatStreamCoordinator,
) : ViewModel() {
    constructor(
        appPreferences: AppPreferences,
        streamCoordinator: ChatStreamCoordinator,
        now: () -> Long,
    ) : this(appPreferences, streamCoordinator) {
        this.now = now
    }

    private var now: () -> Long = { System.currentTimeMillis() }
    private val _uiState = MutableStateFlow(ParallelAgentWorkflowUiState())
    val uiState: StateFlow<ParallelAgentWorkflowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.agents.collect { agents ->
                _uiState.update { state ->
                    val selected = state.selectedAgentIds.ifEmpty { agents.map { it.id }.toSet() }
                        .filter { id -> agents.any { it.id == id } }
                        .toSet()
                        .ifEmpty { agents.map { it.id }.toSet() }
                    state.copy(agents = agents, selectedAgentIds = selected)
                }
            }
        }
    }

    fun onPromptChanged(value: String) {
        _uiState.update { it.copy(prompt = value, error = null) }
    }

    fun toggleAgent(agentId: String) {
        _uiState.update { state ->
            val next = if (agentId in state.selectedAgentIds) {
                state.selectedAgentIds - agentId
            } else {
                state.selectedAgentIds + agentId
            }
            state.copy(selectedAgentIds = next, error = null)
        }
    }

    fun runWorkflow() {
        val state = _uiState.value
        val cleanPrompt = state.prompt.trim()
        if (cleanPrompt.isBlank()) {
            _uiState.update { it.copy(error = "Add a prompt for the workflow.") }
            return
        }
        val selectedAgents = state.agents.filter { it.id in state.selectedAgentIds }
        if (selectedAgents.isEmpty()) {
            _uiState.update { it.copy(error = "Select at least one agent.") }
            return
        }

        val started = selectedAgents.mapIndexed { index, agent ->
            startAgentRun(agent, cleanPrompt, index)
        }
        _uiState.update { it.copy(startedSessionIds = started, prompt = "", error = null) }
    }

    private fun startAgentRun(agent: AgentProfile, prompt: String, index: Int): String {
        val timestamp = now() + index
        val sessionId = newAgentChatSessionId(agent.id, timestamp)
        val userMessage = MessageEntity(
            id = timestamp,
            sessionId = sessionId,
            role = "user",
            content = prompt,
            timestamp = timestamp,
        )
        val assistantMessageId = timestamp + 1_000L
        streamCoordinator.start(
            ChatStreamCommand(
                session = SessionEntity(
                    id = sessionId,
                    title = agent.name,
                    source = "mobile",
                    startedAt = timestamp,
                    endedAt = null,
                    messageCount = 1,
                    model = "hermes-agent",
                    localLastActivityAt = timestamp,
                    lastMessagePreview = prompt,
                ),
                userMessage = userMessage,
                assistantMessageId = assistantMessageId,
                requestBuilder = {
                    ChatCompletionRequest(
                        model = "hermes-agent",
                        messages = listOfNotNull(
                            agent.instructions.trim().takeIf { it.isNotBlank() }?.let {
                                chatRequestMessage("system", it)
                            },
                            chatRequestMessage("user", prompt),
                        ),
                    )
                },
            ),
        )
        return sessionId
    }
}
