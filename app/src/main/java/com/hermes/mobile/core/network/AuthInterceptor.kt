package com.hermes.mobile.core.network

import com.hermes.mobile.core.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = tokenStore.apiKey
        val builder = chain.request().newBuilder()
            .header("ngrok-skip-browser-warning", "true")
        val request = if (apiKey.isBlank() || chain.request().header("Authorization") != null) {
            builder.build()
        } else {
            builder
                .header("Authorization", "Bearer $apiKey")
                .header("x-hermes-session-token", apiKey)
                .build()
        }
        return chain.proceed(request)
    }
}
