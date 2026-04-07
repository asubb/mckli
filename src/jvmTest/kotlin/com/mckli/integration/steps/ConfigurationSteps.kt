package com.mckli.integration.steps

import com.mckli.config.*
import com.mckli.integration.support.TestConfiguration
import io.cucumber.java8.En
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigurationSteps : En {
    private lateinit var configManager: ConfigManager
    private lateinit var validator: ConfigValidator
    private var currentConfig: Configuration? = null
    private var validationErrors: List<String> = emptyList()
    private var lastOperation: Result<Unit> = Result.success(Unit)

    init {
        Before { ->
            TestConfiguration.setup()
            configManager = ConfigManager()
            validator = ConfigValidator()
        }

        Given("a clean configuration directory") {
            cleanConfigDirectory()
            currentConfig = Configuration()
        }

        Given("a server {string} exists with endpoint {string}") { name: String, endpoint: String ->
            val config = currentConfig ?: Configuration()
            val server = ServerConfig(name = name, endpoint = endpoint)
            currentConfig = config.copy(servers = config.servers + server)
            configManager.writeConfig(currentConfig!!)
        }

        When("I add a server with name {string} and endpoint {string}") { name: String, endpoint: String ->
            val config = currentConfig ?: Configuration()
            val server = ServerConfig(name = name, endpoint = endpoint)
            currentConfig = config.copy(servers = config.servers + server)

            validationErrors = validator.validate(currentConfig!!)
            if (validationErrors.isEmpty()) {
                configManager.writeConfig(currentConfig!!)
                lastOperation = Result.success(Unit)
            } else {
                lastOperation = Result.failure(Exception("Validation failed"))
            }
        }

        When("I add a server with name {string} and endpoint {string} and bearer token {string}")
            { name: String, endpoint: String, token: String ->
            val config = currentConfig ?: Configuration()
            val server = ServerConfig(
                name = name,
                endpoint = endpoint,
                auth = AuthConfig.Bearer(token)
            )
            currentConfig = config.copy(servers = config.servers + server)

            validationErrors = validator.validate(currentConfig!!)
            if (validationErrors.isEmpty()) {
                configManager.writeConfig(currentConfig!!)
                lastOperation = Result.success(Unit)
            } else {
                lastOperation = Result.failure(Exception("Validation failed"))
            }
        }

        When("I try to add a server with name {string} and endpoint {string}") { name: String, endpoint: String ->
            val config = currentConfig ?: Configuration()
            val server = ServerConfig(name = name, endpoint = endpoint)
            val testConfig = config.copy(servers = config.servers + server)

            validationErrors = validator.validate(testConfig)
            lastOperation = if (validationErrors.isEmpty()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Validation failed"))
            }
        }

        When("I remove the server {string}") { name: String ->
            val config = currentConfig ?: Configuration()
            currentConfig = config.copy(servers = config.servers.filter { it.name != name })
            configManager.writeConfig(currentConfig!!)
        }

        When("I list all servers") {
            currentConfig = configManager.readConfig()
        }

        When("I set {string} as the default server") { name: String ->
            val config = currentConfig ?: Configuration()
            currentConfig = config.copy(defaultServer = name)
            configManager.writeConfig(currentConfig!!)
        }

        Then("the configuration should contain server {string}") { name: String ->
            val config = currentConfig ?: configManager.readConfig()
            assertNotNull(config)
            assertTrue(config.servers.any { it.name == name }, "Server $name not found")
        }

        Then("the configuration should not contain server {string}") { name: String ->
            val config = currentConfig ?: configManager.readConfig()
            assertFalse(config?.servers?.any { it.name == name } == true, "Server $name should not exist")
        }

        Then("the server {string} should have endpoint {string}") { name: String, endpoint: String ->
            val config = currentConfig ?: configManager.readConfig()
            val server = config?.servers?.find { it.name == name }
            assertNotNull(server, "Server $name not found")
            assertEquals(endpoint, server.endpoint)
        }

        Then("the server {string} should have bearer authentication") { name: String ->
            val config = currentConfig ?: configManager.readConfig()
            val server = config?.servers?.find { it.name == name }
            assertNotNull(server, "Server $name not found")
            assertTrue(server.auth is AuthConfig.Bearer, "Expected bearer auth")
        }

        Then("I should see {int} servers") { count: Int ->
            val config = currentConfig ?: configManager.readConfig()
            assertEquals(count, config?.servers?.size ?: 0)
        }

        Then("I should see server {string}") { name: String ->
            val config = currentConfig ?: configManager.readConfig()
            assertTrue(config?.servers?.any { it.name == name } == true)
        }

        Then("the configuration validation should fail") {
            assertTrue(lastOperation.isFailure || validationErrors.isNotEmpty())
        }

        Then("I should see an error about invalid URL scheme") {
            assertTrue(validationErrors.any { it.contains("http://") || it.contains("https://") })
        }

        Then("the default server should be {string}") { name: String ->
            val config = currentConfig ?: configManager.readConfig()
            assertEquals(name, config?.defaultServer)
        }
    }

    private fun cleanConfigDirectory() {
        val configPath = TestConfiguration.tempDir
        configPath.listFiles()?.forEach { it.deleteRecursively() }
    }
}
