package com.hermes.mobile.core.model

import com.hermes.mobile.core.util.escapeJson

enum class HermesFeatureActionKind {
    Read,
    Create,
    Update,
    Delete,
    Command,
}

data class HermesFeatureAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: HermesFeatureActionKind,
    val target: String,
    val bodyTemplate: String = "{}",
)

data class HermesFeatureCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val actions: List<HermesFeatureAction>,
)

val hermesFeatureCatalog: List<HermesFeatureCategory> = listOf(
    HermesFeatureCategory(
        id = "chat",
        title = "Chat",
        subtitle = "Native chat, continuation headers, slash commands",
        actions = listOf(
            read("chat.capabilities", "API capabilities", "Real upstream API contract", "v1/capabilities"),
            read("chat.models", "Available API model", "OpenAI-compatible model list", "v1/models"),
            command("chat.new", "New chat", "Start fresh session", "/new"),
            command("chat.reset", "Reset", "Reset current session", "/reset"),
            command("chat.retry", "Retry", "Retry last turn", "/retry"),
            command("chat.undo", "Undo", "Remove last exchange", "/undo"),
            command("chat.stop", "Stop", "Interrupt current work", "/stop"),
            command("chat.background", "Background", "Continue active work in background", "/background"),
            command("chat.queue", "Queue", "Queue the next instruction while busy", "/queue "),
            command("chat.steer", "Steer", "Steer the active response", "/steer "),
            command("chat.title", "Rename chat", "Set current session title", "/title "),
            command("chat.resume", "Resume", "Resume by session id or title", "/resume "),
            command("chat.rollback", "Rollback", "Rollback recent work", "/rollback"),
            command("chat.compress", "Compress", "Compact context", "/compress"),
        ),
    ),
    HermesFeatureCategory(
        id = "memory",
        title = "Memory",
        subtitle = "Persistent memory through Hermes agent runs",
        actions = listOf(
            run("memory.summary", "What Hermes knows", "Ask server-side Hermes to summarize remembered user/project facts", "Show what you know about me from persistent memory. Include projects, preferences, environment, and uncertainty."),
            run("memory.projects", "Project memory", "Recall current projects and active context", "List the projects you remember, current working directories, important constraints, and open risks."),
            run("memory.add", "Add memory", "Save durable memory through Hermes learning loop", "Remember this as durable user/project memory: "),
            command("memory.insights", "Insights", "Usage and memory-derived insights", "/insights"),
            command("memory.usage", "Usage", "Current session token/cost usage", "/usage"),
        ),
    ),
    HermesFeatureCategory(
        id = "runs",
        title = "Runs",
        subtitle = "Structured API runs with status, events, approvals, stop",
        actions = listOf(
            run("runs.start", "Start native run", "POST /v1/runs returns run_id", "Run a concise server health and capability check. Report model, enabled tools, and what mobile can control."),
            read("runs.capabilities", "Run capability map", "Discover run endpoints", "v1/capabilities"),
            command("runs.stop", "Stop active run", "Interrupt from chat", "/stop"),
            command("runs.status", "Status", "Current gateway/session status", "/status"),
            command("runs.approve", "Approve", "Approve pending tool/action request", "/approve"),
            command("runs.deny", "Deny", "Deny pending tool/action request", "/deny"),
        ),
    ),
    HermesFeatureCategory(
        id = "skills",
        title = "Skills",
        subtitle = "Installed skills and agentskills.io hub through native slash commands",
        actions = listOf(
            command("skills.list", "Installed skills", "List skills available to Hermes", "/skills"),
            command("skills.search", "Search skills", "Search skill hub", "/skills search "),
            command("skills.browse", "Browse hub", "Browse agentskills.io-compatible hub", "/skills browse"),
            command("skills.inspect", "Inspect skill", "Open SKILL.md content by name", "/skills inspect "),
            command("skills.install", "Install skill", "Install skill by id/link", "/skills install "),
            command("skills.reload", "Reload skills", "Rescan local skill library", "/reload-skills"),
        ),
    ),
    HermesFeatureCategory(
        id = "tools",
        title = "Tools",
        subtitle = "Toolsets, browser, terminal, web, delegation",
        actions = listOf(
            command("tools.list", "Toolsets", "List enabled/disabled toolsets", "/tools list"),
            command("tools.enable", "Enable toolset", "Enable toolset by name", "/tools enable "),
            command("tools.disable", "Disable toolset", "Disable toolset by name", "/tools disable "),
            command("tools.reloadMcp", "Reload MCP", "Reload MCP tool configuration", "/reload-mcp"),
            command("tools.browser", "Browser", "Browser automation command", "/browser"),
            run("tools.audit", "Tool audit", "Ask Hermes to report real enabled tools", "List enabled toolsets for api_server, available MCP tools, browser/web support, and any disabled critical tools."),
        ),
    ),
    HermesFeatureCategory(
        id = "automations",
        title = "Automations",
        subtitle = "Real /api/jobs cron endpoints plus /cron command",
        actions = listOf(
            read("automations.list", "List jobs", "GET /api/jobs", "api/jobs?include_disabled=true"),
            create(
                "automations.create",
                "Create job",
                "POST /api/jobs",
                "api/jobs",
                """{"name":"Mobile test job","schedule":"daily at 9am","prompt":"Send a short Hermes status report.","deliver":"local"}""",
            ),
            command("automations.cron", "Cron help", "Show cron command help", "/cron"),
            command("automations.pause", "Pause job", "Pause by job id", "/cron pause "),
            command("automations.resume", "Resume job", "Resume by job id", "/cron resume "),
            command("automations.run", "Run job now", "Trigger by job id", "/cron run "),
        ),
    ),
    HermesFeatureCategory(
        id = "platforms",
        title = "Platforms",
        subtitle = "Gateway status and cross-platform continuation",
        actions = listOf(
            command("platforms.status", "Status", "Gateway/platform status", "/status"),
            command("platforms.list", "Platforms", "Platform overview", "/platforms"),
            command("platforms.topic", "Telegram topics", "Inspect Telegram DM topic sessions", "/topic"),
            command("platforms.sethome", "Set home", "Set current platform home", "/sethome"),
            run("platforms.continuation", "Continuation check", "Ask Hermes how this mobile session maps to gateway sessions", "Explain how this mobile API session can continue work from Telegram/Discord/Slack using session ids and session keys."),
        ),
    ),
    HermesFeatureCategory(
        id = "models",
        title = "Models",
        subtitle = "Model list, switcher, usage, insights",
        actions = listOf(
            read("models.api", "API model", "GET /v1/models", "v1/models"),
            command("models.current", "Current model", "Show current provider/model", "/model"),
            command("models.switch", "Switch model", "Use /model provider:model", "/model "),
            command("models.reasoning", "Reasoning", "Set reasoning effort if backend supports it", "/reasoning "),
            command("models.usage", "Usage", "Current usage/cost", "/usage"),
            command("models.insights", "Insights", "Usage analytics", "/insights"),
        ),
    ),
    HermesFeatureCategory(
        id = "security",
        title = "Server",
        subtitle = "Health, detailed status, update, doctor",
        actions = listOf(
            read("security.health", "Health", "GET /health", "health"),
            read("security.detailed", "Detailed health", "GET /health/detailed", "health/detailed"),
            command("security.reload", "Reload", "Reload Hermes config/env", "/reload"),
            run("security.doctor", "Doctor", "Run Hermes diagnostic through agent", "Run a Hermes doctor-style diagnostic. Check API server, model auth, gateway status, tool availability, and common mobile connection issues."),
            run("security.update", "Update guidance", "Ask Hermes to run/update if safe", "Check whether hermes update is needed. If update is available, explain safest command and whether unattended update is safe."),
        ),
    ),
)

private fun read(id: String, title: String, subtitle: String, target: String) =
    HermesFeatureAction(id, title, subtitle, HermesFeatureActionKind.Read, target)

private fun create(id: String, title: String, subtitle: String, target: String, bodyTemplate: String = "{}") =
    HermesFeatureAction(id, title, subtitle, HermesFeatureActionKind.Create, target, bodyTemplate)

private fun command(id: String, title: String, subtitle: String, command: String) =
    HermesFeatureAction(id, title, subtitle, HermesFeatureActionKind.Command, command, command)

private fun run(id: String, title: String, subtitle: String, input: String) =
    HermesFeatureAction(
        id = id,
        title = title,
        subtitle = subtitle,
        kind = HermesFeatureActionKind.Create,
        target = "v1/runs",
        bodyTemplate = """{"input":"${input.escapeJson()}"}""",
    )
