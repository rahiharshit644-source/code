package com.example.domain.agent

import com.example.core.model.ChangeType
import com.example.core.model.FileDiffItem
import com.example.core.model.RiskLevel
import java.util.UUID

object DiffEngine {

    /**
     * Generates a standard unified diff representation between original and proposed content.
     */
    fun generateUnifiedDiff(filePath: String, originalContent: String, proposedContent: String): String {
        val oldLines = if (originalContent.isEmpty()) emptyList() else originalContent.lines()
        val newLines = if (proposedContent.isEmpty()) emptyList() else proposedContent.lines()

        val sb = StringBuilder()
        sb.append("--- a/$filePath\n")
        sb.append("+++ b/$filePath\n")

        // Simple Myers-style LCS diff output for line-by-line review
        val diffList = computeSimpleDiff(oldLines, newLines)

        var hunkStarted = false
        for (item in diffList) {
            if (!hunkStarted) {
                sb.append("@@ -1,${oldLines.size} +1,${newLines.size} @@\n")
                hunkStarted = true
            }
            sb.append(item).append("\n")
        }

        if (!hunkStarted) {
            sb.append("@@ -0,0 +0,0 @@\n (No textual changes detected)\n")
        }

        return sb.toString()
    }

    private fun computeSimpleDiff(oldLines: List<String>, newLines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        var j = 0

        while (i < oldLines.size || j < newLines.size) {
            if (i < oldLines.size && j < newLines.size && oldLines[i] == newLines[j]) {
                result.add(" " + oldLines[i])
                i++
                j++
            } else if (j < newLines.size && (i >= oldLines.size || !oldLines.contains(newLines[j]))) {
                result.add("+" + newLines[j])
                j++
            } else if (i < oldLines.size) {
                result.add("-" + oldLines[i])
                i++
            } else {
                j++
            }
        }
        return result
    }

    /**
     * Evaluates risk level based on affected file path and change operation.
     */
    fun evaluateRiskLevel(filePath: String, changeType: ChangeType): RiskLevel {
        if (changeType == ChangeType.DELETE) return RiskLevel.HIGH
        val lower = filePath.lowercase()
        if (lower.contains("build.gradle") || lower.contains("manifest") || lower.contains("settings.gradle") || lower.contains(".env")) {
            return RiskLevel.HIGH
        }
        if (lower.endsWith(".kt") || lower.endsWith(".java") || lower.endsWith(".py") || lower.endsWith(".ts") || lower.endsWith(".js")) {
            return RiskLevel.MEDIUM
        }
        return RiskLevel.LOW
    }

    /**
     * Parses AI model response containing structured file modifications:
     * Supports XML format:
     * <file_change path="path/to/file" type="create|modify|delete" reason="...">
     * content
     * </file_change>
     *
     * And markdown code block format:
     * ```file:path/to/file
     * content
     * ```
     */
    fun parseModelChanges(rawResponse: String, originalContentProvider: (filePath: String) -> String): List<FileDiffItem> {
        val items = mutableListOf<FileDiffItem>()

        // 1. Try structured file_change tags. Parse attributes independently so their order is flexible.
        val xmlRegex = Regex("<file_change\\s+([^>]*)>([\\s\\S]*?)</file_change>", RegexOption.IGNORE_CASE)
        val attributeRegex = Regex("([A-Za-z_]+)=\"([^\"]*)\"")
        val xmlMatches = xmlRegex.findAll(rawResponse).toList()
        if (xmlMatches.isNotEmpty()) {
            for (match in xmlMatches) {
                val attributes = attributeRegex.findAll(match.groupValues[1]).associate {
                    it.groupValues[1].lowercase() to it.groupValues[2]
                }
                val path = attributes["path"]?.trim()?.removePrefix("/") ?: continue
                val typeStr = attributes["type"].orEmpty().trim().lowercase()
                val reason = attributes["reason"].orEmpty().trim().ifEmpty { "Agent proposed change" }
                val oldPath = attributes["old_path"]?.trim()?.removePrefix("/")?.takeIf { it.isNotBlank() }
                val content = match.groupValues[2].trimIndent()

                val changeType = when (typeStr) {
                    "create" -> ChangeType.CREATE
                    "delete" -> ChangeType.DELETE
                    "rename" -> ChangeType.RENAME
                    else -> ChangeType.MODIFY
                }
                if (changeType == ChangeType.RENAME && oldPath == null) continue

                val originalPath = if (changeType == ChangeType.RENAME) oldPath!! else path
                val original = if (changeType == ChangeType.CREATE) "" else originalContentProvider(originalPath)
                val diff = generateUnifiedDiff(path, original, if (changeType == ChangeType.DELETE) "" else content)
                val risk = evaluateRiskLevel(path, changeType)

                items.add(
                    FileDiffItem(
                        id = UUID.randomUUID().toString(),
                        filePath = path,
                        oldPath = oldPath,
                        changeType = changeType,
                        originalContent = original,
                        proposedContent = content,
                        diffUnified = diff,
                        reason = reason,
                        riskLevel = risk,
                        isApproved = false
                    )
                )
            }
            return items
        }

        // 2. Try markdown format ```file:app/src/main/java/... or ```filepath:...
        val mdRegex = Regex("```(?:file:)?([a-zA-Z0-9_./\\-]+)\\n([\\s\\S]*?)```")
        val mdMatches = mdRegex.findAll(rawResponse).toList()

        for (match in mdMatches) {
            val rawPath = match.groupValues[1].trim()
            // Ignore language specifiers like ```kotlin or ```java unless it contains a file extension or slash
            if (rawPath.contains(".") || rawPath.contains("/")) {
                val path = rawPath.removePrefix("file:").removePrefix("/")
                val content = match.groupValues[2].trimIndent()
                val original = originalContentProvider(path)
                val changeType = if (original.isEmpty()) ChangeType.CREATE else ChangeType.MODIFY
                val diff = generateUnifiedDiff(path, original, content)
                val risk = evaluateRiskLevel(path, changeType)

                items.add(
                    FileDiffItem(
                        id = UUID.randomUUID().toString(),
                        filePath = path,
                        changeType = changeType,
                        originalContent = original,
                        proposedContent = content,
                        diffUnified = diff,
                        reason = "Modified based on task requirements",
                        riskLevel = risk,
                        isApproved = false
                    )
                )
            }
        }

        return items
    }
}
