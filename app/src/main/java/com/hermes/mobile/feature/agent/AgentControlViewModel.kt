package com.hermes.mobile.feature.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.error.ErrorMapper
import com.hermes.mobile.core.model.HermesFeatureAction
import com.hermes.mobile.core.model.HermesFeatureActionKind
import com.hermes.mobile.core.model.HermesFeatureCategory
import com.hermes.mobile.core.model.hermesFeatureCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val initialCategory = hermesFeatureCatalog.first { it.id == "memory" }
private val initialAction = initialCategory.actions.first()

data class AgentControlUiState(
    val categories: List<HermesFeatureCategory> = hermesFeatureCatalog,
    val selectedCategory: HermesFeatureCategory = initialCategory,
    val selectedAction: HermesFeatureAction = initialAction,
    val body: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AgentControlViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentControlUiState())
    val uiState: StateFlow<AgentControlUiState> = _uiState.asStateFlow()

    init {
        prepareOrRun(initialAction)
    }

    fun selectCategory(category: HermesFeatureCategory) {
        val action = category.actions.first()
        _uiState.update {
            it.copy(
                selectedCategory = category,
                selectedAction = action,
                body = "",
                response = "",
                error = null,
            )
        }
        prepareOrRun(action)
    }

    fun selectAction(action: HermesFeatureAction) {
        _uiState.update {
            it.copy(selectedAction = action, body = "", response = "", error = null)
        }
        prepareOrRun(action)
    }

    fun onBodyChanged(value: String) {
        _uiState.update { it.copy(body = value, error = null) }
    }

    fun runSelectedAction() {
        val state = _uiState.value
        runAction(state.selectedAction, state.body.ifBlank { state.selectedAction.uiBodyTemplate() })
    }

    private fun prepareOrRun(action: HermesFeatureAction) {
        when (action.kind) {
            HermesFeatureActionKind.Read -> runAction(action, action.bodyTemplate)
            HermesFeatureActionKind.Command -> _uiState.update {
                it.copy(
                    body = commandPrompt(action.target),
                    response = "Tap Run to ask Hermes to perform this action on the connected agent.",
                )
            }
            HermesFeatureActionKind.Create,
            HermesFeatureActionKind.Update,
            HermesFeatureActionKind.Delete -> _uiState.update {
                it.copy(
                    body = action.uiBodyTemplate(),
                    response = if (action.target == "v1/runs") {
                        "Edit instruction if needed, then Run."
                    } else {
                        "Edit details if needed, then Run."
                    },
                )
            }
        }
    }

    private fun runAction(action: HermesFeatureAction, body: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, response = "") }
            val requestBody = action.requestBody(body)
            val result = when (action.kind) {
                HermesFeatureActionKind.Read -> repository.getText(action.target)
                HermesFeatureActionKind.Create -> repository.postText(action.target, requestBody)
                HermesFeatureActionKind.Update -> repository.putText(action.target, requestBody)
                HermesFeatureActionKind.Delete -> repository.deleteText(action.target)
                HermesFeatureActionKind.Command -> repository.postText("v1/runs", runRequestBody(body))
            }
            result
                .onSuccess { text ->
                    _uiState.update {
                        it.copy(
                            body = if (action.kind == HermesFeatureActionKind.Read) text else it.body,
                            response = text.ifBlank { "${action.title} complete" },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = ErrorMapper.userMessage(error, "${action.title} failed")) }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun HermesFeatureAction.requestBody(body: String): String {
        val trimmed = body.trim()
        if (trimmed.startsWith("{")) return trimmed
        return if (target == "v1/runs") runRequestBody(trimmed.ifBlank { uiBodyTemplate() }) else trimmed.ifBlank { bodyTemplate }
    }

    private fun runRequestBody(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("{")) return trimmed
        return """{"input":"${trimmed.escapeJson()}"}"""
    }

    private fun HermesFeatureAction.uiBodyTemplate(): String {
        if (target != "v1/runs") return bodyTemplate
        return Regex(""""input"\s*:\s*"((?:\\.|[^"])*)"""")
            .find(bodyTemplate)
            ?.groupValues
            ?.getOrNull(1)
            ?.unescapeJson()
            ?: bodyTemplate
    }

    private fun commandPrompt(command: String): String {
        return "Hermes native command requested from mobile: $command\n" +
            "Run the equivalent Hermes desktop/gateway action if available. " +
            "Return concise output plus any next action the mobile user can take."
    }

    private fun String.escapeJson(): String {
        return buildString(length) {
            this@escapeJson.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
        }
    }

    private fun String.unescapeJson(): String {
        return replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
