package com.example.core.storage

import android.content.Context
import com.example.core.model.ChangeType
import com.example.core.model.FileDiffItem
import com.example.core.model.ProjectFile
import java.io.File
import java.util.UUID

class ProjectFileManager(private val context: Context) {

    private val baseProjectsDir: File
        get() = File(context.filesDir, "projects")
            .apply { if (!exists()) mkdirs() }

    private val baseBackupsDir: File
        get() = File(context.filesDir, "backups")
            .apply { if (!exists()) mkdirs() }

    private fun validateProjectId(projectId: String) {
        require(
            projectId.isNotBlank() &&
                projectId.matches(Regex("[A-Za-z0-9_-]{1,128}"))
        ) {
            "Invalid project ID"
        }
    }

    fun getProjectDir(projectId: String): File {
        validateProjectId(projectId)

        val root = baseProjectsDir.canonicalFile
        val project = File(root, projectId).canonicalFile

        require(project.path.startsWith(root.path + File.separator)) {
            "Project directory must remain inside the projects root"
        }

        if (!project.exists() && !project.mkdirs() && !project.isDirectory) {
            throw IllegalStateException("Could not create project directory")
        }

        return project
    }

    private fun getProjectBackupsDir(projectId: String): File {
        validateProjectId(projectId)

        val root = baseBackupsDir.canonicalFile
        val projectBackups = File(root, projectId).canonicalFile

        require(projectBackups.path.startsWith(root.path + File.separator)) {
            "Backup directory must remain inside the backups root"
        }

        return projectBackups.apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Resolves a relative path safely inside the project directory.
     */
    fun resolveSafeFile(projectId: String, relativePath: String): File {
        val projectDir = getProjectDir(projectId).canonicalFile
        val cleanRelative = relativePath.trim()
            .removePrefix("/")
            .removePrefix("\\")

        val target = File(projectDir, cleanRelative).canonicalFile

        if (
            target.path != projectDir.path &&
            !target.path.startsWith(projectDir.path + File.separator)
        ) {
            throw SecurityException(
                "Access denied: Path '$relativePath' traverses outside project root!"
            )
        }

        return target
    }

    fun listProjectFiles(
        projectId: String,
        currentRelativePath: String = ""
    ): List<ProjectFile> {
        val root = getProjectDir(projectId)

        val targetDir = if (currentRelativePath.isEmpty()) {
            root
        } else {
            resolveSafeFile(projectId, currentRelativePath)
        }

        if (!targetDir.exists() || !targetDir.isDirectory) {
            return emptyList()
        }

        val files = targetDir.listFiles() ?: return emptyList()

        return files
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            .map { file ->
                val rel = root.toURI()
                    .relativize(file.toURI())
                    .path
                    .removeSuffix("/")

                ProjectFile(
                    relativePath = rel,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    lastModified = file.lastModified()
                )
            }
    }

    fun listAllProjectFilesRecursive(
        projectId: String,
        maxFiles: Int = 1000
    ): List<ProjectFile> {
        val root = getProjectDir(projectId)
        val result = mutableListOf<ProjectFile>()

        fun walk(dir: File) {
            if (result.size >= maxFiles) return

            val list = dir.listFiles() ?: return

            for (f in list.sortedWith(
                compareBy({ !it.isDirectory }, { it.name.lowercase() })
            )) {
                if (result.size >= maxFiles) return

                if (f.name in setOf(".git", "build", ".gradle")) {
                    continue
                }

                val rel = root.toURI()
                    .relativize(f.toURI())
                    .path
                    .removeSuffix("/")

                result.add(
                    ProjectFile(
                        relativePath = rel,
                        name = f.name,
                        isDirectory = f.isDirectory,
                        sizeBytes = if (f.isDirectory) 0L else f.length(),
                        lastModified = f.lastModified()
                    )
                )

                if (f.isDirectory) {
                    walk(f)
                }
            }
        }

        walk(root)
        return result
    }

    fun searchProject(
        projectId: String,
        query: String,
        searchContent: Boolean = false
    ): List<ProjectFile> {
        val all = listAllProjectFilesRecursive(projectId)
        val lowerQuery = query.trim().lowercase()

        if (lowerQuery.isEmpty()) return all

        return all.filter { file ->
            if (
                file.name.lowercase().contains(lowerQuery) ||
                file.relativePath.lowercase().contains(lowerQuery)
            ) {
                true
            } else if (
                searchContent &&
                !file.isDirectory &&
                file.sizeBytes < 500_000
            ) {
                try {
                    readFileContent(projectId, file.relativePath)
                        .contains(query, ignoreCase = true)
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }
        }
    }

    fun readFileContent(
        projectId: String,
        relativePath: String,
        maxBytes: Int = 1_000_000
    ): String {
        val file = resolveSafeFile(projectId, relativePath)

        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $relativePath")
        }

        if (file.isDirectory) {
            throw IllegalArgumentException("Target is a directory: $relativePath")
        }

        if (file.length() > maxBytes) {
            return file.bufferedReader().use { reader ->
                val chars = CharArray(maxBytes)
                val read = reader.read(chars).coerceAtLeast(0)

                String(chars, 0, read) +
                    "\n\n// ... [File truncated: exceeds display limit] ..."
            }
        }

        return file.readText(Charsets.UTF_8)
    }

    fun writeFileContent(
        projectId: String,
        relativePath: String,
        content: String
    ) {
        val file = resolveSafeFile(projectId, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    fun createDirectory(projectId: String, relativePath: String): Boolean {
        val dir = resolveSafeFile(projectId, relativePath)
        return dir.mkdirs()
    }

    fun renameFile(
        projectId: String,
        oldRelativePath: String,
        newRelativePath: String
    ): Boolean {
        val oldFile = resolveSafeFile(projectId, oldRelativePath)
        val newFile = resolveSafeFile(projectId, newRelativePath)

        if (!oldFile.exists()) return false

        newFile.parentFile?.mkdirs()
        return oldFile.renameTo(newFile)
    }

    fun deleteFile(projectId: String, relativePath: String): Boolean {
        val file = resolveSafeFile(projectId, relativePath)

        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun deleteProjectFiles(projectId: String): Boolean {
        val dir = getProjectDir(projectId)
        val backupDir = getProjectBackupsDir(projectId)

        backupDir.deleteRecursively()
        return dir.deleteRecursively()
    }

    /**
     * Creates a snapshot backup of selected files.
     */
    fun createBackup(
        projectId: String,
        filesToBackup: List<String>
    ): String {
        val backupId = UUID.randomUUID().toString()
        val backupRoot = getProjectBackupsDir(projectId).canonicalFile

        val backupDir = File(backupRoot, backupId).apply {
            if (!mkdirs() && !isDirectory) {
                throw IllegalStateException("Could not create backup directory")
            }
        }

        try {
            for (rel in filesToBackup.distinct()) {
                val originalFile = resolveSafeFile(projectId, rel)

                if (originalFile.exists() && !originalFile.isDirectory) {
                    val targetBackupFile = File(backupDir, rel).canonicalFile

                    if (
                        targetBackupFile.path != backupDir.path &&
                        !targetBackupFile.path.startsWith(
                            backupDir.path + File.separator
                        )
                    ) {
                        throw SecurityException(
                            "Backup path escapes backup directory"
                        )
                    }

                    targetBackupFile.parentFile?.mkdirs()
                    originalFile.copyTo(targetBackupFile, overwrite = true)
                }
            }

            return backupId
        } catch (e: Exception) {
            backupDir.deleteRecursively()
            throw e
        }
    }

    /**
     * Restores files from a backup snapshot.
     */
    fun restoreBackup(projectId: String, backupId: String): Boolean {
        if (!backupId.matches(Regex("[A-Za-z0-9_-]{1,128}"))) {
            return false
        }

        val backupRoot = getProjectBackupsDir(projectId).canonicalFile
        val backupDir = File(backupRoot, backupId).canonicalFile

        if (!backupDir.path.startsWith(backupRoot.path + File.separator)) {
            return false
        }

        if (!backupDir.exists()) return false

        getProjectDir(projectId)

        val createdPathsManifest = File(
            backupDir,
            ".codepilot-created-paths"
        )

        if (createdPathsManifest.isFile) {
            try {
                createdPathsManifest.readLines(Charsets.UTF_8)
                    .filter { it.isNotBlank() }
                    .forEach { relativePath ->
                        deleteFile(projectId, relativePath)
                    }
            } catch (_: Exception) {
                return false
            }
        }

        fun restoreRecursively(source: File) {
            val list = source.listFiles() ?: return

            for (f in list) {
                if (f == createdPathsManifest) continue

                if (f.isDirectory) {
                    restoreRecursively(f)
                } else {
                    val rel = backupDir.toURI()
                        .relativize(f.toURI())
                        .path

                    val target = resolveSafeFile(projectId, rel)
                    target.parentFile?.mkdirs()
                    f.copyTo(target, overwrite = true)
                }
            }
        }

        restoreRecursively(backupDir)
        return true
    }

    /**
     * Applies approved file changes and creates a backup first.
     */
    fun applyChangeSet(
        projectId: String,
        diffItems: List<FileDiffItem>
    ): Result<String> {
        val approved = diffItems.filter { it.isApproved }

        return try {
            if (approved.isEmpty()) {
                return Result.failure(
                    IllegalStateException("No approved changes to apply.")
                )
            }

            approved.forEach { item ->
                require(
                    !item.filePath.contains('\n') &&
                        !item.filePath.contains('\r')
                ) {
                    "File paths cannot contain line breaks"
                }

                val target = resolveSafeFile(projectId, item.filePath)

                when (item.changeType) {
                    ChangeType.CREATE -> require(!target.exists()) {
                        "Cannot create '${item.filePath}': a file or folder already exists."
                    }

                    ChangeType.MODIFY, ChangeType.DELETE ->
                        require(target.exists() && !target.isDirectory) {
                            "Cannot ${item.changeType.name.lowercase()} '${item.filePath}': file does not exist."
                        }

                    ChangeType.RENAME -> {
                        val oldPath = item.oldPath
                            ?: throw IllegalArgumentException(
                                "Rename is missing oldPath"
                            )

                        val oldFile = resolveSafeFile(projectId, oldPath)

                        require(oldFile.exists() && !oldFile.isDirectory) {
                            "Rename source '$oldPath' does not exist."
                        }

                        require(!target.exists()) {
                            "Rename destination '${item.filePath}' already exists."
                        }
                    }
                }
            }

            val pathsToBackup = approved.mapNotNull { item ->
                when (item.changeType) {
                    ChangeType.MODIFY, ChangeType.DELETE -> item.filePath
                    ChangeType.RENAME -> item.oldPath
                    ChangeType.CREATE -> null
                }
            }

            val backupId = createBackup(projectId, pathsToBackup)

            val createdPaths = approved
                .filter {
                    it.changeType == ChangeType.CREATE ||
                        it.changeType == ChangeType.RENAME
                }
                .map { it.filePath }

            // FIX: File(parent, child) accepts two arguments.
            // Create the backup directory first, then its manifest file.
            val manifestFile = File(
                File(getProjectBackupsDir(projectId), backupId),
                ".codepilot-created-paths"
            )

            manifestFile.writeText(
                createdPaths.joinToString("\n"),
                Charsets.UTF_8
            )

            try {
                for (item in approved) {
                    when (item.changeType) {
                        ChangeType.CREATE, ChangeType.MODIFY -> {
                            writeFileContent(
                                projectId,
                                item.filePath,
                                item.proposedContent
                            )
                        }

                        ChangeType.RENAME -> {
                            val oldPath = item.oldPath
                                ?: throw IllegalArgumentException(
                                    "Rename is missing oldPath"
                                )

                            check(renameFile(projectId, oldPath, item.filePath)) {
                                "Could not rename '${oldPath}' to '${item.filePath}'."
                            }

                            if (item.proposedContent.isNotEmpty()) {
                                writeFileContent(
                                    projectId,
                                    item.filePath,
                                    item.proposedContent
                                )
                            }
                        }

                        ChangeType.DELETE -> {
                            check(deleteFile(projectId, item.filePath)) {
                                "Could not delete '${item.filePath}'."
                            }
                        }
                    }
                }

                Result.success(backupId)
            } catch (applyError: Exception) {
                runCatching {
                    restoreBackup(projectId, backupId)
                }

                approved.forEach { item ->
                    if (
                        item.changeType == ChangeType.CREATE ||
                        item.changeType == ChangeType.RENAME
                    ) {
                        runCatching {
                            deleteFile(projectId, item.filePath)
                        }
                    }
                }

                throw applyError
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates starter files for common project templates.
     */
    fun initProjectTemplate(
        projectId: String,
        templateType: String,
        projectName: String
    ) {
        when (templateType.lowercase()) {
            "android", "compose" -> {
                writeFileContent(
                    projectId,
                    "app/src/main/java/com/example/app/MainActivity.kt",
                    """
                    package com.example.app

                    import android.os.Bundle
                    import androidx.activity.ComponentActivity
                    import androidx.activity.compose.setContent
                    import androidx.compose.foundation.layout.*
                    import androidx.compose.material3.*
                    import androidx.compose.runtime.Composable
                    import androidx.compose.ui.Modifier
                    import androidx.compose.ui.unit.dp

                    class MainActivity : ComponentActivity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            setContent {
                                MaterialTheme {
                                    Surface(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        GreetingView("$projectName")
                                    }
                                }
                            }
                        }
                    }

                    @Composable
                    fun GreetingView(name: String) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Welcome to ${'$'}name",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Built with CodePilot AI & Jetpack Compose."
                            )
                        }
                    }
                    """.trimIndent()
                )

                writeFileContent(
                    projectId,
                    "app/build.gradle.kts",
                    """
                    plugins {
                        alias(libs.plugins.android.application)
                        alias(libs.plugins.kotlin.compose)
                    }

                    android {
                        namespace = "com.example.app"
                        compileSdk = 35

                        defaultConfig {
                            applicationId = "com.example.app"
                            minSdk = 24
                            targetSdk = 35
                            versionCode = 1
                            versionName = "1.0"
                        }
                    }
                    """.trimIndent()
                )

                writeFileContent(
                    projectId,
                    "README.md",
                    "# $projectName\n\nAndroid Jetpack Compose project created with CodePilot AI."
                )
            }

            "python" -> {
                writeFileContent(
                    projectId,
                    "main.py",
                    """
                    import sys

                    def main():
                        print("Hello from $projectName!")
                        print("Python CLI powered by CodePilot AI.")

                    if __name__ == "__main__":
                        main()
                    """.trimIndent()
                )

                writeFileContent(
                    projectId,
                    "requirements.txt",
                    "pytest>=7.0.0\nrequests>=2.31.0\n"
                )

                writeFileContent(
                    projectId,
                    "test_main.py",
                    """
                    def test_sample():
                        assert 1 + 1 == 2
                    """.trimIndent()
                )

                writeFileContent(
                    projectId,
                    "README.md",
                    "# $projectName\n\nPython utility application initialized with CodePilot AI."
                )
            }

            "react", "typescript" -> {
                writeFileContent(
                    projectId,
                    "src/App.tsx",
                    """
                    import React, { useState } from 'react';

                    export const App: React.FC = () => {
                      const [count, setCount] = useState(0);

                      return (
                        <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
                          <h1>$projectName</h1>
                          <p>Interactive web application engineered by CodePilot AI.</p>
                          <button onClick={() => setCount(c => c + 1)}>Clicks: {count}</button>
                        </div>
                      );
                    };
                    """.trimIndent()
                )

                writeFileContent(
                    projectId,
                    "package.json",
                    """
                    {
                      "name": "${projectName.lowercase().replace(" ", "-")}",
                      "version": "1.0.0",
                      "scripts": {
                        "dev": "vite",
                        "build": "vite build",
                        "test": "vitest"
                      },
                      "dependencies": {
                        "react": "^18.3.0",
                        "react-dom": "^18.3.0"
                      }
                    }
                    """.trimIndent()
                )

                writeFileContent(
                    projectId,
                    "README.md",
                    "# $projectName\n\nReact + TypeScript application created with CodePilot AI."
                )
            }

            else -> {
                writeFileContent(
                    projectId,
                    "src/Main.kt",
                    """
                    fun main() {
                        println("Hello from $projectName!")
                        println("CodePilot AI Agent Ready.")
                    }
                    """.trimIndent()
                )

                writeFileContent(
                    projectId,
                    "README.md",
                    "# $projectName\n\nProject initialized with CodePilot AI."
                )
            }
        }
    }
}
