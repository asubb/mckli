package com.mckli.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ToolMetadata(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonElement? = null
) {
    fun getCompactDescription(): String? {
        val desc = description ?: return null
        val firstLine = desc.lineSequence().firstOrNull() ?: ""
        val isTruncatedByLine = desc.contains('\n')
        val isTruncatedByLength = firstLine.length > 200

        return when {
            isTruncatedByLength -> firstLine.take(197) + "..."
            isTruncatedByLine -> firstLine + "..."
            else -> firstLine
        }
    }
}

@Serializable
data class ToolList(
    val tools: List<ToolMetadata>
)
