package com.hermes.mobile.core.settings

enum class HermesGlassRole {
    Navigation,
    ReadablePanel,
    ChatBubble,
    Action,
    Status,
}

data class HermesGlassType(
    val role: HermesGlassRole,
    val label: String,
    val blurScale: Float,
    val refractionHeightScale: Float,
    val refractionAmountScale: Float,
    val surfaceAlphaScale: Float,
    val radiusDelta: Float,
    val elastic: Boolean,
    val tintsGlass: Boolean = false,
)

val hermesGlassTypes = listOf(
    HermesGlassType(
        role = HermesGlassRole.Navigation,
        label = "Elastic nav",
        blurScale = 0.95f,
        refractionHeightScale = 1.18f,
        refractionAmountScale = 1.24f,
        surfaceAlphaScale = 0.74f,
        radiusDelta = 10f,
        elastic = true,
    ),
    HermesGlassType(
        role = HermesGlassRole.ReadablePanel,
        label = "Readable panel",
        blurScale = 1.12f,
        refractionHeightScale = 0.82f,
        refractionAmountScale = 0.72f,
        surfaceAlphaScale = 1.24f,
        radiusDelta = 0f,
        elastic = false,
    ),
    HermesGlassType(
        role = HermesGlassRole.ChatBubble,
        label = "Chat bubble",
        blurScale = 0.86f,
        refractionHeightScale = 0.74f,
        refractionAmountScale = 0.66f,
        surfaceAlphaScale = 1.08f,
        radiusDelta = -5f,
        elastic = false,
    ),
    HermesGlassType(
        role = HermesGlassRole.Action,
        label = "Action glass",
        blurScale = 0.90f,
        refractionHeightScale = 1.0f,
        refractionAmountScale = 1.08f,
        surfaceAlphaScale = 0.86f,
        radiusDelta = -2f,
        elastic = true,
    ),
    HermesGlassType(
        role = HermesGlassRole.Status,
        label = "Status glass",
        blurScale = 1.0f,
        refractionHeightScale = 0.70f,
        refractionAmountScale = 0.62f,
        surfaceAlphaScale = 1.06f,
        radiusDelta = -8f,
        elastic = false,
    ),
)

fun hermesGlassTypeForRole(role: HermesGlassRole): HermesGlassType {
    return hermesGlassTypes.first { it.role == role }
}
