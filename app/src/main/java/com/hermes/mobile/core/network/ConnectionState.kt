package com.hermes.mobile.core.network

sealed interface ConnectionState {
    data object Connected : ConnectionState
    data object Connecting : ConnectionState
    data object Disconnected : ConnectionState
    data class Error(val message: String) : ConnectionState
}
