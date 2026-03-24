package com.mckli.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ToolMetadata(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonElement? = null
)

@Serializable
data class ToolList(
    val tools: List<ToolMetadata>
)
