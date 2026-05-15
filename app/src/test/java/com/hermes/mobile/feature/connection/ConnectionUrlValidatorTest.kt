package com.hermes.mobile.feature.connection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ConnectionUrlValidatorTest {
    @Test
    fun acceptsHttpsRemoteEndpoint() {
        assertEquals(
            "https://agent.example",
            normalizeHermesServerUrl(" https://agent.example/ "),
        )
    }

    @Test
    fun acceptsFirstCleanPastedEndpointLine() {
        assertEquals(
            "https://agent.example/api",
            normalizeHermesServerUrl("\n https://agent.example/api/\nignore-this-line "),
        )
    }

    @Test
    fun acceptsLocalHttpForEmulatorDevelopment() {
        assertEquals(
            "http://10.0.2.2:8000",
            normalizeHermesServerUrl("http://10.0.2.2:8000/"),
        )
        assertEquals(
            "http://127.0.0.1:8000",
            normalizeHermesServerUrl("http://127.0.0.1:8000"),
        )
        assertEquals(
            "http://[::1]:8000",
            normalizeHermesServerUrl("http://[::1]:8000/"),
        )
    }

    @Test
    fun addsHttpSchemeForBareLocalEndpoints() {
        assertEquals(
            "http://10.0.2.2:8000",
            normalizeHermesServerUrl("10.0.2.2:8000"),
        )
        assertEquals(
            "http://localhost:8000",
            normalizeHermesServerUrl("localhost:8000/"),
        )
        assertEquals(
            "http://[::1]:8000",
            normalizeHermesServerUrl("[::1]:8000"),
        )
        assertEquals(
            "http://[::1]:8000",
            normalizeHermesServerUrl("::1:8000"),
        )
        assertEquals(
            "http://localhost",
            normalizeHermesServerUrl("localhost?debug=true#local"),
        )
        assertEquals(
            "http://[::1]",
            normalizeHermesServerUrl("::1?debug=true#local"),
        )
    }

    @Test
    fun addsHttpsSchemeForBareRemoteEndpoints() {
        assertEquals(
            "https://agent.example",
            normalizeHermesServerUrl(" agent.example/ "),
        )
        assertEquals(
            "https://agent.example:8443/api",
            normalizeHermesServerUrl("agent.example:8443/api"),
        )
    }

    @Test
    fun removesQueryAndFragmentFromBaseEndpoint() {
        assertEquals(
            "https://agent.example/api",
            normalizeHermesServerUrl("https://agent.example/api?token=abc#setup"),
        )
        assertEquals(
            "http://10.0.2.2:8000",
            normalizeHermesServerUrl("10.0.2.2:8000?debug=true#local"),
        )
    }

    @Test
    fun rejectsRemoteHttpEndpoint() {
        assertNull(normalizeHermesServerUrl("http://agent.example"))
    }

    @Test
    fun rejectsUnsupportedSchemes() {
        assertNull(normalizeHermesServerUrl("ftp://agent.example"))
        assertNull(normalizeHermesServerUrl("ws://agent.example"))
    }

    @Test
    fun rejectsOutOfRangePorts() {
        assertNull(normalizeHermesServerUrl("https://agent.example:99999"))
        assertNull(normalizeHermesServerUrl("localhost:70000"))
    }

    @Test
    fun rejectsUrlsWithEmbeddedCredentials() {
        assertNull(normalizeHermesServerUrl("https://user:pass@agent.example"))
        assertNull(normalizeHermesServerUrl("http://token@localhost:8000"))
    }
}
