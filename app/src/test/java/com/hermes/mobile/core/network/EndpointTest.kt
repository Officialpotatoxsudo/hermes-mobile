package com.hermes.mobile.core.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EndpointTest {
    @Test
    fun endpointJoinsBaseAndPath() {
        assertEquals("https://agent.example/v1/sessions", "https://agent.example/".endpoint("/v1/sessions"))
    }

    @Test
    fun endpointPreservesQuery() {
        assertEquals(
            "https://agent.example/v1/sessions?limit=50&offset=0",
            "https://agent.example".endpoint("v1/sessions?limit=50&offset=0"),
        )
    }

    @Test
    fun endpointDoesNotDuplicateV1WhenBaseAlreadyIncludesIt() {
        assertEquals(
            "https://agent.example/v1/chat/completions",
            "https://agent.example/v1".endpoint("v1/chat/completions"),
        )
    }
}
