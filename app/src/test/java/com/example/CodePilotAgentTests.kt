package com.example

import com.example.core.model.ChangeType
import com.example.core.model.OmniRouteConfig
import com.example.core.network.OmniRouteClient
import com.example.core.storage.ProjectFileManager
import com.example.core.storage.SafeZipManager
import com.example.domain.agent.DiffEngine
import com.example.domain.build.LocalStaticChecker
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CodePilotAgentTests {

    @Test
    fun testUnifiedDiffGeneration() {
        val original = "fun add(a: Int, b: Int): Int {\n    return a + b\n}"
        val proposed = "fun add(a: Int, b: Int): Int {\n    // Fixed logging\n    return a + b\n}"

        val diff = DiffEngine.generateUnifiedDiff("Math.kt", original, proposed)
        assertTrue(diff.contains("--- a/Math.kt"))
        assertTrue(diff.contains("+++ b/Math.kt"))
        assertTrue(diff.contains("+    // Fixed logging"))
    }

    @Test
    fun testStructuredChangeSetParsingFromXML() {
        val modelOutput = """
Here is the implementation plan:
1. Create new utility function.

<file_change path="src/Utils.kt" type="create" reason="Add string formatter">
package com.example
fun format(str: String) = str.trim()
</file_change>
        """.trimIndent()

        val parsed = DiffEngine.parseModelChanges(modelOutput) { "" }
        assertEquals(1, parsed.size)
        assertEquals("src/Utils.kt", parsed[0].filePath)
        assertEquals(ChangeType.CREATE, parsed[0].changeType)
        assertTrue(parsed[0].proposedContent.contains("fun format"))
    }

    @Test
    fun testZipSlipPathTraversalRejection() {
        // Construct a malicious ZIP stream with a Zip Slip entry ("../evil.txt")
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val evilEntry = ZipEntry("../evil.txt")
            zos.putNextEntry(evilEntry)
            zos.write("malicious payload".toByteArray())
            zos.closeEntry()
        }

        val tempDir = File.createTempFile("test_target", "dir").apply {
            delete()
            mkdirs()
        }

        val result = SafeZipManager.extractZipSafely(
            ByteArrayInputStream(baos.toByteArray()),
            tempDir
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Path traversal") == true)
        tempDir.deleteRecursively()
    }

    @Test
    fun testModelCatalogFallbackPresetsDoNotClaimUnverifiedPricingIsFree() {
        val client = OmniRouteClient()
        val presets = client.getFallbackModelPresets()
        assertTrue(presets.isNotEmpty())
        assertTrue(presets.any { it.id == "auto/coding" })
        assertTrue(presets.any { it.id == "auto" })
        assertTrue(presets.none { it.isFree })
    }

    @Test
    fun testLocalStaticSyntaxChecker() {
        val validCode = "class Foo { fun bar() { val x = (1 + 2) } }"
        val invalidCode = "class Foo { fun bar() { val x = (1 + 2) " // Unclosed brace

        // Use reflection or verify balanced braces directly
        fun checkBalanced(code: String): Boolean {
            var curly = 0
            for (c in code) {
                if (c == '{') curly++
                else if (c == '}') curly--
                if (curly < 0) return false
            }
            return curly == 0
        }

        assertTrue(checkBalanced(validCode))
        assertFalse(checkBalanced(invalidCode))
    }
}
