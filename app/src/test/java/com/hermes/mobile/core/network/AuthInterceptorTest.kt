package com.hermes.mobile.core.network

import com.hermes.mobile.core.auth.TokenStore
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthInterceptorTest {
    private val server = MockWebServer()

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun trimsPastedApiKeyBeforeAuthorizationHeader() {
        val tokenStore = mockk<TokenStore>()
        every { tokenStore.apiKey } returns "  api-key  "
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .build()
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        client.newCall(Request.Builder().url(server.url("/health")).build()).execute().use { response ->
            assertEquals(200, response.code)
        }

        assertEquals("Bearer api-key", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun usesFirstSafePastedApiKeyLine() {
        val tokenStore = mockk<TokenStore>()
        every { tokenStore.apiKey } returns "\n api-key\nInjected: bad "
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .build()
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))

        client.newCall(Request.Builder().url(server.url("/health")).build()).execute().use { response ->
            assertEquals(200, response.code)
        }

        assertEquals("Bearer api-key", server.takeRequest().headers["Authorization"])
    }
}
