package com.mckli.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigValidatorTest {
    private val validator = ConfigValidator()

    @Test
    fun `validate accepts valid configuration`() {
        val config = Configuration(
            servers = listOf(
                ServerConfig(
                    name = "test",
                    endpoint = "https://example.com/api",
                    timeout = 30000,
                    poolSize = 10
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.isEmpty(), "Valid config should have no errors")
    }

    @Test
    fun `validate rejects invalid URL scheme`() {
        val config = Configuration(
            servers = listOf(
                ServerConfig(
                    name = "test",
                    endpoint = "ftp://example.com/api"
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("http://") || it.contains("https://") })
    }

    @Test
    fun `validate rejects negative timeout`() {
        val config = Configuration(
            servers = listOf(
                ServerConfig(
                    name = "test",
                    endpoint = "https://example.com/api",
                    timeout = -1
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("timeout") && it.contains("positive") })
    }

    @Test
    fun `validate rejects zero pool size`() {
        val config = Configuration(
            servers = listOf(
                ServerConfig(
                    name = "test",
                    endpoint = "https://example.com/api",
                    poolSize = 0
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("pool size") && it.contains("positive") })
    }

    @Test
    fun `validate rejects duplicate server names`() {
        val config = Configuration(
            servers = listOf(
                ServerConfig(name = "test", endpoint = "https://example.com/api1"),
                ServerConfig(name = "test", endpoint = "https://example.com/api2")
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("Duplicate server name") })
    }

    @Test
    fun `validate rejects invalid default server`() {
        val config = Configuration(
            servers = listOf(
                ServerConfig(name = "test", endpoint = "https://example.com/api")
            ),
            defaultServer = "nonexistent"
        )

        val errors = validator.validate(config)
        assertTrue(errors.any { it.contains("Default server") && it.contains("not found") })
    }

    @Test
    fun `validate accepts valid default server`() {
        val config = Configuration(
            servers = listOf(
                ServerConfig(name = "test", endpoint = "https://example.com/api")
            ),
            defaultServer = "test"
        )

        val errors = validator.validate(config)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validate collects multiple errors`() {
        val config = Configuration(
            servers = listOf(
                ServerConfig(
                    name = "test1",
                    endpoint = "ftp://bad.com",
                    timeout = -1,
                    poolSize = 0
                ),
                ServerConfig(
                    name = "test1", // duplicate
                    endpoint = "https://good.com"
                )
            )
        )

        val errors = validator.validate(config)
        assertTrue(errors.size >= 3, "Should collect multiple errors")
    }
}
