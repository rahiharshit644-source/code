package com.example.domain.agent

import com.example.core.storage.ProjectFileManager

class ProjectContextIndexer(private val fileManager: ProjectFileManager) {

    data class ContextBundle(
        val fileTreeSummary: String,
        val totalFileCount: Int,
        val keyFilesContent: Map<String, String>,
        val estimatedTokens: Int
    )

    fun buildContext(projectId: String, userPrompt: String, maxCharBudget: Int = 40_000): ContextBundle {
        val allFiles = fileManager.listAllProjectFilesRecursive(projectId)
        val fileTreeSb = StringBuilder()
        var totalFiles = 0

        for (f in allFiles.take(150)) {
            val prefix = if (f.isDirectory) "📁 " else "📄 "
            fileTreeSb.append(prefix).append(f.relativePath).append("\n")
            if (!f.isDirectory) totalFiles++
        }
        if (allFiles.size > 150) {
            fileTreeSb.append("... [${allFiles.size - 150} more files omitted for brevity]\n")
        }

        val prioritizedFiles = mutableListOf<String>()

        // 1. Core manifests
        val manifestCandidates = listOf(
            "README.md",
            "app/build.gradle.kts",
            "build.gradle.kts",
            "app/src/main/AndroidManifest.xml",
            "package.json",
            "requirements.txt"
        )
        for (m in manifestCandidates) {
            if (allFiles.any { it.relativePath == m && !it.isDirectory }) {
                prioritizedFiles.add(m)
            }
        }

        // 2. Query keywords matching files
        val promptWords = userPrompt.lowercase()
            .split(Regex("[^a-zA-Z0-9_]"))
            .filter { it.length > 3 }

        for (f in allFiles) {
            if (!f.isDirectory && !prioritizedFiles.contains(f.relativePath)) {
                val pathLower = f.relativePath.lowercase()
                if (promptWords.any { pathLower.contains(it) }) {
                    prioritizedFiles.add(f.relativePath)
                }
            }
        }

        // 3. Fallback to entry files if list is sparse
        if (prioritizedFiles.size < 4) {
            for (f in allFiles.take(10)) {
                if (!f.isDirectory && !prioritizedFiles.contains(f.relativePath)) {
                    prioritizedFiles.add(f.relativePath)
                }
            }
        }

        var currentChars = fileTreeSb.length
        val loadedContents = mutableMapOf<String, String>()

        for (path in prioritizedFiles) {
            if (currentChars >= maxCharBudget) break
            try {
                val content = fileManager.readFileContent(projectId, path, maxBytes = 15_000)
                if (currentChars + content.length <= maxCharBudget) {
                    loadedContents[path] = content
                    currentChars += content.length
                }
            } catch (ignored: Exception) {
            }
        }

        val estimatedTokens = currentChars / 4

        return ContextBundle(
            fileTreeSummary = fileTreeSb.toString(),
            totalFileCount = totalFiles,
            keyFilesContent = loadedContents,
            estimatedTokens = estimatedTokens
        )
    }
}
