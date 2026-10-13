package com.example.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val rootPath: String, // Relative to app's internal filesDir/projects/<id>
    val primaryLanguage: String = "Kotlin",
    val fileCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val currentBranch: String = "main",
    val githubRepoUrl: String? = null
)

@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val prompt: String,
    val modelUsed: String,
    val status: String, // PENDING, PLANNING, EDITING, COMPLETED, FAILED, ROLLED_BACK
    val summary: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val changedFilesJson: String = "[]",
    val rollbackBackupId: String? = null
)

data class ProjectFile(
    val relativePath: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val children: List<ProjectFile> = emptyList()
)

enum class ChangeType {
    CREATE,
    MODIFY,
    RENAME,
    DELETE
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class FileDiffItem(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val oldPath: String? = null,
    val changeType: ChangeType,
    val originalContent: String = "",
    val proposedContent: String = "",
    val diffUnified: String = "",
    val reason: String = "",
    val riskLevel: RiskLevel = RiskLevel.LOW,
    var isApproved: Boolean = false
)

data class ProposedChangeSet(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val explanation: String,
    val files: List<FileDiffItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class FindingSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

data class ReviewFinding(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val severity: FindingSeverity,
    val affectedFile: String,
    val lineNumber: Int? = null,
    val codeSnippet: String = "",
    val explanation: String,
    val suggestedFix: String = "",
    val isVerified: Boolean = true
)

data class ProjectReviewReport(
    val projectId: String,
    val projectName: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val healthScore: Int = 85,
    val findings: List<ReviewFinding> = emptyList(),
    val summary: String = ""
)

data class BuildResult(
    val buildId: String = UUID.randomUUID().toString(),
    val profile: String = "Debug Build",
    val success: Boolean,
    val exitCode: Int,
    val durationMs: Long,
    val passedTests: Int = 0,
    val failedTests: Int = 0,
    val skippedTests: Int = 0,
    val logs: String = "",
    val artifactPath: String? = null,
    val artifactName: String? = null
)

data class OmniRouteConfig(
    val baseUrl: String = "http://10.0.2.2:20128/v1", // Default for Android Emulator to access host machine OmniRoute
    val apiKey: String = "",
    val selectedModel: String = "auto/coding", // Default OmniRoute routing preset
    val autoFallback: Boolean = true,
    val fallbackModel: String = "auto",
    val timeoutSeconds: Int = 60,
    val contextBudgetTokens: Int = 16384,
    val streamResponse: Boolean = true,
    val temperature: Float = 0.2f
)

data class ModelOption(
    val id: String,
    val name: String,
    val provider: String = "OmniRoute",
    val isFree: Boolean = true,
    val isCodingSpecialized: Boolean = false,
    val description: String = ""
)

data class GitHubRepo(
    val id: Long,
    val name: String,
    val fullName: String,
    val description: String?,
    val defaultBranch: String,
    val isPrivate: Boolean,
    val htmlUrl: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val planSteps: List<String> = emptyList(),
    val associatedChangeSet: ProposedChangeSet? = null,
    val isStreaming: Boolean = false,
    val modelBadge: String? = null
)

enum class MessageSender {
    USER,
    AGENT,
    SYSTEM
}
