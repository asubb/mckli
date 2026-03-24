package com.mckli.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerConfigTest {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `ServerConfig serializes to JSON`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api",
            auth = AuthConfig.Bearer("token123"),
            timeout = 30000,
            poolSize = 10
        )

        val jsonString = json.encodeToString(ServerConfig.serializer(), config)

        println("Serialized JSON: $jsonString")

        assert(jsonString.contains("\"name\"")) { "JSON missing 'name': $jsonString" }
        assert(jsonString.contains("\"endpoint\"")) { "JSON missing 'endpoint': $jsonString" }
        assert(jsonString.contains("\"auth\"")) { "JSON missing 'auth': $jsonString" }
        assert(jsonString.contains("\"timeout\"")) { "JSON missing 'timeout': $jsonString" }
        assert(jsonString.contains("\"poolSize\"")) { "JSON missing 'poolSize': $jsonString" }
    }

    @Test
    fun `ServerConfig deserializes from JSON`() {
        val jsonString = """
            {
              "name": "test",
              "endpoint": "https://example.com/api",
              "timeout": 30000,
              "poolSize": 10
            }
        """.trimIndent()

        val config = json.decodeFromString(ServerConfig.serializer(), jsonString)

        assertEquals("test", config.name)
        assertEquals("https://example.com/api", config.endpoint)
        assertEquals(30000, config.timeout)
        assertEquals(10, config.poolSize)
        assertNull(config.auth)
    }

    @Test
    fun `ServerConfig with Basic auth serializes correctly`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api",
            auth = AuthConfig.Basic("user", "pass")
        )

        val jsonString = json.encodeToString(ServerConfig.serializer(), config)
        val decoded = json.decodeFromString<ServerConfig>(jsonString)

        assert(decoded.auth is AuthConfig.Basic)
        assertEquals("user", (decoded.auth as AuthConfig.Basic).username)
        assertEquals("pass", (decoded.auth as AuthConfig.Basic).password)
    }

    @Test
    fun `ServerConfig with Bearer auth serializes correctly`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api",
            auth = AuthConfig.Bearer("mytoken")
        )

        val jsonString = json.encodeToString(ServerConfig.serializer(), config)
        val decoded = json.decodeFromString<ServerConfig>(jsonString)

        assert(decoded.auth is AuthConfig.Bearer)
        assertEquals("mytoken", (decoded.auth as AuthConfig.Bearer).token)
    }

    @Test
    fun `Configuration with multiple servers serializes correctly`() {
        val config = Configuration(
            servers = listOf(
                ServerConfig(name = "server1", endpoint = "https://api1.com"),
                ServerConfig(name = "server2", endpoint = "https://api2.com")
            ),
            defaultServer = "server1"
        )

        val jsonString = json.encodeToString(Configuration.serializer(), config)
        val decoded = json.decodeFromString(Configuration.serializer(), jsonString)

        assertEquals(2, decoded.servers.size)
        assertEquals("server1", decoded.defaultServer)
        assertEquals("server1", decoded.servers[0].name)
        assertEquals("server2", decoded.servers[1].name)
    }

    @Test
    fun `ServerConfig uses default values`() {
        val config = ServerConfig(
            name = "test",
            endpoint = "https://example.com/api"
        )

        assertEquals(30000, config.timeout)
        assertEquals(10, config.poolSize)
        assertNull(config.auth)
    }
}
