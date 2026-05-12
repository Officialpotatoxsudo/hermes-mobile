Task statement
- Improve Hermes mobile UI/UX for actual Hermes agent use from phone.

Desired outcome
- ChatGPT/iOS-like UI with translucent/blurred containers.
- GUI-first controls instead of exposing raw commands.
- Chat screen moves above keyboard on focus.
- AI replies have border and clean markdown, no token usage footer.
- Proper animated loading/typing indicators.
- Setup and agent control stay easy and real.

Known facts/evidence
- App is Android Compose.
- Current chat UI already has Lottie raw assets for typing/loading.
- Current integration uses upstream Hermes API surfaces: /health, /health/detailed, /v1/models, /v1/capabilities, /v1/runs, /api/jobs, /v1/chat/completions.
- User provided target drawer/menu style and asked for iOS-like translucent polish.

Constraints
- Do not add new dependencies unless necessary.
- No screenshots; user wants to provide output manually.
- Keep changes scoped and verified.
- No mock features or fake endpoints.

Unknowns/open questions
- Exact brand imagery/assets are not available yet.
- Upstream Hermes does not expose all desktop actions as first-class HTTP endpoints; GUI must map available actions to real endpoints/runs.

Likely codebase touchpoints
- app/src/main/java/com/hermes/mobile/feature/chat/ChatShellScreen.kt
- app/src/main/java/com/hermes/mobile/feature/home/HomeScreen.kt
- app/src/main/java/com/hermes/mobile/feature/agent/AgentControlScreen.kt
- app/src/main/java/com/hermes/mobile/feature/agent/AgentControlViewModel.kt
- app/src/main/java/com/hermes/mobile/core/model/HermesFeatureCatalog.kt
- app/src/main/java/com/hermes/mobile/navigation/HermesNavGraph.kt
