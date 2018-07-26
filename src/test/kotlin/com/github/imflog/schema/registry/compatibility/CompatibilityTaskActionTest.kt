package com.github.imflog.schema.registry.compatibility

import com.github.imflog.schema.registry.StringFileSubject
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import org.apache.avro.Schema
import org.assertj.core.api.Assertions
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class CompatibilityTaskActionTest {

    lateinit var folderRule: TemporaryFolder

    @Before
    fun setUp() {
        folderRule = TemporaryFolder()
        folderRule.create()
    }

    @After
    fun tearDown() {
        folderRule.delete()
    }

    @Test
    fun `Should verify compatibility`() {
        // given
        val parser = Schema.Parser()
        val testSchema = parser.parse("{\"type\": \"record\", \"name\": \"test\", \"fields\": [{ \"name\": \"name\", \"type\": \"string\" }]}")
        val registryClient = MockSchemaRegistryClient()
        registryClient.register("test", testSchema)
        folderRule.newFolder("src", "main", "avro", "external")

        val subjects = arrayListOf(StringFileSubject("test", "src/main/avro/external/test.avsc"))
        File(folderRule.root, "src/main/avro/external/test.avsc").writeText("""
            {"type": "record",
             "name": "test",
             "fields": [
                {"name": "name", "type": "string" },
                {"name": "newField", "type": "string", "default": ""}
             ]
            }
        """.trimIndent())

        // when
        val errorCount = CompatibilityTaskAction(
                registryClient,
                subjects,
                folderRule.root
        ).run()

        // then
        Assertions.assertThat(errorCount).isEqualTo(0)
    }

    @Test
    fun `Should fail on incompatible schemas`() {
        // given
        val parser = Schema.Parser()
        val testSchema = parser.parse("{\"type\": \"record\", \"name\": \"test\", \"fields\": [{ \"name\": \"name\", \"type\": \"string\" }]}")
        val registryClient = MockSchemaRegistryClient()
        registryClient.register("test", testSchema)
        folderRule.newFolder("src", "main", "avro", "external")

        val subjects = arrayListOf(StringFileSubject("test", "src/main/avro/external/test.avsc"))
        File(folderRule.root, "src/main/avro/external/test.avsc").writeText("""
            {"type": "record",
             "name": "test",
             "fields": [
                {"name": "name", "type": "boolean" }
             ]
            }
        """.trimIndent())

        // when
        val errorCount = CompatibilityTaskAction(
                registryClient,
                subjects,
                folderRule.root
        ).run()

        // then
        Assertions.assertThat(errorCount).isEqualTo(1)
    }

}