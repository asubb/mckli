package com.mckli.tools

import kotlinx.serialization.json.*
import kotlin.test.*

class ArgumentParserTest {
    private val parser = ArgumentParser()

    @Test
    fun `should parse string argument`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("name") {
                    put("type", "string")
                }
            }
        }
        val args = listOf("--name", "testvalue")

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("testvalue", parsed["name"]?.jsonPrimitive?.content)
    }

    @Test
    fun `should parse integer argument`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("count") {
                    put("type", "integer")
                }
            }
        }
        val args = listOf("--count", "42")

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(42, parsed["count"]?.jsonPrimitive?.int)
    }

    @Test
    fun `should parse boolean argument with true value`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("enabled") {
                    put("type", "boolean")
                }
            }
        }
        val args = listOf("--enabled", "true")

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(true, parsed["enabled"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `should parse boolean flag without value`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("verbose") {
                    put("type", "boolean")
                }
            }
        }
        val args = listOf("--verbose")

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(true, parsed["verbose"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `should parse array argument`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("tags") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                }
            }
        }
        val args = listOf("--tags", "tag1,tag2,tag3")

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        val tags = parsed["tags"]?.jsonArray
        assertNotNull(tags)
        assertEquals(3, tags.size)
        assertEquals("tag1", tags[0].jsonPrimitive.content)
    }

    @Test
    fun `should parse multiple arguments`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("name") { put("type", "string") }
                putJsonObject("age") { put("type", "integer") }
                putJsonObject("active") { put("type", "boolean") }
            }
        }
        val args = listOf("--name", "Alice", "--age", "30", "--active", "true")

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("Alice", parsed["name"]?.jsonPrimitive?.content)
        assertEquals(30, parsed["age"]?.jsonPrimitive?.int)
        assertEquals(true, parsed["active"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `should fail for invalid integer value`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("count") {
                    put("type", "integer")
                }
            }
        }
        val args = listOf("--count", "not-a-number")

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `should validate required arguments`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("name") { put("type", "string") }
            }
            putJsonArray("required") {
                add("name")
            }
        }
        val args = emptyList<String>()

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("required") == true)
    }

    @Test
    fun `should pass validation when required arguments provided`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("name") { put("type", "string") }
            }
            putJsonArray("required") {
                add("name")
            }
        }
        val args = listOf("--name", "test")

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `should handle optional arguments`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("name") { put("type", "string") }
                putJsonObject("age") { put("type", "integer") }
            }
            putJsonArray("required") {
                add("name")
            }
        }
        val args = listOf("--name", "test")

        // When
        val result = parser.parseArguments(schema, args)

        // Then
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("test", parsed["name"]?.jsonPrimitive?.content)
        assertNull(parsed["age"])
    }

    @Test
    fun `should parse JSON input directly`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("config") {
                    put("type", "object")
                }
            }
        }
        val jsonInput = """{"config": {"key": "value", "nested": {"foo": "bar"}}}"""

        // When
        val result = parser.parseJsonInput(schema, jsonInput)

        // Then
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        val config = parsed["config"]?.jsonObject
        assertNotNull(config)
        assertEquals("value", config["key"]?.jsonPrimitive?.content)
    }

    @Test
    fun `should validate JSON input against schema`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("name") { put("type", "string") }
            }
            putJsonArray("required") {
                add("name")
            }
        }
        val jsonInput = """{"age": 30}"""

        // When
        val result = parser.parseJsonInput(schema, jsonInput)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `should handle enum validation`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("status") {
                    put("type", "string")
                    putJsonArray("enum") {
                        add("active")
                        add("inactive")
                        add("pending")
                    }
                }
            }
        }
        val validArgs = listOf("--status", "active")
        val invalidArgs = listOf("--status", "invalid")

        // When
        val validResult = parser.parseArguments(schema, validArgs)
        val invalidResult = parser.parseArguments(schema, invalidArgs)

        // Then
        assertTrue(validResult.isSuccess)
        assertTrue(invalidResult.isFailure)
    }

    @Test
    fun `should handle number with minimum constraint`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("age") {
                    put("type", "integer")
                    put("minimum", 0)
                }
            }
        }
        val validArgs = listOf("--age", "25")
        val invalidArgs = listOf("--age", "-5")

        // When
        val validResult = parser.parseArguments(schema, validArgs)
        val invalidResult = parser.parseArguments(schema, invalidArgs)

        // Then
        assertTrue(validResult.isSuccess)
        assertTrue(invalidResult.isFailure)
    }

    @Test
    fun `should handle string with pattern constraint`() {
        // Given
        val schema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("email") {
                    put("type", "string")
                    put("pattern", "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$")
                }
            }
        }
        val validArgs = listOf("--email", "test@example.com")
        val invalidArgs = listOf("--email", "not-an-email")

        // When
        val validResult = parser.parseArguments(schema, validArgs)
        val invalidResult = parser.parseArguments(schema, invalidArgs)

        // Then
        assertTrue(validResult.isSuccess)
        assertTrue(invalidResult.isFailure)
    }
}

/**
 * Helper class for parsing and validating tool arguments.
 * This would be part of the actual implementation.
 */
class ArgumentParser {
    
    fun parseArguments(schema: JsonObject, args: List<String>): Result<JsonObject> {
        return runCatching {
            val properties = schema["properties"]?.jsonObject
                ?: return@runCatching buildJsonObject {}
            
            val required = schema["required"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet()
                ?: emptySet()
            
            val parsed = mutableMapOf<String, JsonElement>()
            var i = 0
            
            while (i < args.size) {
                val arg = args[i]
                if (!arg.startsWith("--")) {
                    i++
                    continue
                }
                
                val propName = arg.removePrefix("--")
                val propSchema = properties[propName]?.jsonObject
                    ?: throw IllegalArgumentException("Unknown argument: $propName")
                
                val type = propSchema["type"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("No type for argument: $propName")
                
                when (type) {
                    "boolean" -> {
                        // Boolean can be flag or explicit value
                        val value = if (i + 1 < args.size && !args[i + 1].startsWith("--")) {
                            i++
                            args[i].toBoolean()
                        } else {
                            true
                        }
                        parsed[propName] = JsonPrimitive(value)
                    }
                    else -> {
                        i++
                        if (i >= args.size) {
                            throw IllegalArgumentException("Missing value for argument: $propName")
                        }
                        val value = args[i]
                        parsed[propName] = parseValue(type, value, propSchema)
                    }
                }
                
                i++
            }
            
            // Validate required fields
            val missing = required - parsed.keys
            if (missing.isNotEmpty()) {
                throw IllegalArgumentException("Missing required arguments: ${missing.joinToString()}")
            }
            
            buildJsonObject {
                parsed.forEach { (key, value) -> put(key, value) }
            }
        }
    }
    
    fun parseJsonInput(schema: JsonObject, jsonInput: String): Result<JsonObject> {
        return runCatching {
            val parsed = Json.parseToJsonElement(jsonInput).jsonObject
            validateAgainstSchema(parsed, schema)
            parsed
        }
    }
    
    private fun parseValue(type: String, value: String, schema: JsonObject): JsonElement {
        return when (type) {
            "string" -> {
                validateString(value, schema)
                JsonPrimitive(value)
            }
            "integer" -> {
                val intValue = value.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid integer: $value")
                validateNumber(intValue, schema)
                JsonPrimitive(intValue)
            }
            "number" -> {
                val doubleValue = value.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid number: $value")
                validateNumber(doubleValue, schema)
                JsonPrimitive(doubleValue)
            }
            "boolean" -> {
                JsonPrimitive(value.toBoolean())
            }
            "array" -> {
                val items = value.split(",").map { JsonPrimitive(it.trim()) }
                JsonArray(items)
            }
            else -> JsonPrimitive(value)
        }
    }
    
    private fun validateString(value: String, schema: JsonObject) {
        val enum = schema["enum"]?.jsonArray
        if (enum != null) {
            val validValues = enum.map { it.jsonPrimitive.content }
            if (value !in validValues) {
                throw IllegalArgumentException("Invalid value: $value. Must be one of: ${validValues.joinToString()}")
            }
        }
        
        val pattern = schema["pattern"]?.jsonPrimitive?.content
        if (pattern != null) {
            val regex = Regex(pattern)
            if (!regex.matches(value)) {
                throw IllegalArgumentException("Value does not match pattern: $pattern")
            }
        }
    }
    
    private fun validateNumber(value: Number, schema: JsonObject) {
        val minimum = schema["minimum"]?.jsonPrimitive?.doubleOrNull
        if (minimum != null && value.toDouble() < minimum) {
            throw IllegalArgumentException("Value $value is less than minimum: $minimum")
        }
        
        val maximum = schema["maximum"]?.jsonPrimitive?.doubleOrNull
        if (maximum != null && value.toDouble() > maximum) {
            throw IllegalArgumentException("Value $value is greater than maximum: $maximum")
        }
    }
    
    private fun validateAgainstSchema(json: JsonObject, schema: JsonObject) {
        val required = schema["required"]?.jsonArray?.map { it.jsonPrimitive.content }?.toSet()
            ?: emptySet()
        
        val missing = required - json.keys
        if (missing.isNotEmpty()) {
            throw IllegalArgumentException("Missing required fields: ${missing.joinToString()}")
        }
    }
}
