package com.hermes.mobile.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.mobile.core.data.HermesRepository
import com.hermes.mobile.core.model.SendPaymentRequest
import com.hermes.mobile.core.model.SendPaymentResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SendUiState(
    val recipientAddress: String = "",
    val amount: String = "",
    val selectedNetwork: String = "matic",
    val description: String = "",
    val isSending: Boolean = false,
    val paymentUrl: String = "",
    val paymentId: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class SendViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    val availableNetworks = listOf("matic", "eth", "bsc", "arbitrum", "optimism")

    fun onRecipientAddressChanged(value: String) {
        _uiState.update { it.copy(recipientAddress = value, error = null, isSuccess = false) }
    }

    fun onAmountChanged(value: String) {
        _uiState.update { it.copy(amount = value, error = null, isSuccess = false) }
    }

    fun onNetworkSelected(network: String) {
        _uiState.update { it.copy(selectedNetwork = network, error = null, isSuccess = false) }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value, error = null, isSuccess = false) }
    }

    fun composePaymentRequest() {
        val state = _uiState.value
        val address = state.recipientAddress.trim()
        val amount = state.amount.trim()

        when {
            address.isEmpty() -> {
                _uiState.update { it.copy(error = "Recipient address is required") }
                return
            }
            amount.isEmpty() -> {
                _uiState.update { it.copy(error = "Amount is required") }
                return
            }
            amount.toDoubleOrNull() == null -> {
                _uiState.update { it.copy(error = "Please enter a valid amount") }
                return
            }
            address.length < 10 -> {
                _uiState.update { it.copy(error = "Please enter a valid recipient address") }
                return
            }
        }

        sendPaymentRequest(address, amount, state.selectedNetwork, state.description)
    }

    private fun sendPaymentRequest(
        recipientAddress: String,
        amount: String,
        network: String,
        description: String,
    ) {
        val request = SendPaymentRequest(
            recipientAddress = recipientAddress,
            amount = amount,
            network = network,
            description = description,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null, isSuccess = false, paymentUrl = "", paymentId = "") }
            repository.sendPaymentRequest(request)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            paymentUrl = response.paymentUrl,
                            paymentId = response.id,
                            isSuccess = true,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = error.message ?: "Payment request failed",
                        )
                    }
                }
        }
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                recipientAddress = "",
                amount = "",
                description = "",
                isSending = false,
                paymentUrl = "",
                paymentId = "",
                error = null,
                isSuccess = false,
            )
        }
    }
}
