package com.mckli.transport

import com.mckli.http.McpRequest
import com.mckli.http.McpResponse

interface McpTransport {
    suspend fun sendRequest(request: McpRequest): Result<McpResponse>
    fun close()
}
