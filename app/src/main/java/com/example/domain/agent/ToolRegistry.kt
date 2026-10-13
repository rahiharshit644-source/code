package com.example.domain.agent

import com.example.core.model.FileDiffItem
import com.example.core.storage.ProjectFileManager

data class ToolDefinition(
    val name: String,
    val description: String,
    val parametersSchema: String,
    val requiresApproval: Boolean
)

data class ToolExecutionResult(
    val toolName: String,
    val success: Boolean,
    val output: String,
    val requiresUserApproval: Boolean = false
)

class ToolRegistry(private val fileManager: ProjectFileManager) {

    val availableTools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "list_project_files",
            description = "Lists files and directories under the given relative path.",
            parametersSchema = "{ \"path\": \"string\" }",
            requiresApproval = false
        ),
        ToolDefinition(
            name = "read_project_file",
            description = "Reads content of a source file within the project boundary.",
            parametersSchema = "{ \"path\": \"string\" }",
            requiresApproval = false
        ),
        ToolDefinition(
            name = "search_project",
            description = "Searches files by name or content grep inside the project.",
            parametersSchema = "{ \"query\": \"string\", \"searchContent\": boolean }",
            requiresApproval = false
        ),
        ToolDefinition(
            name = "propose_patch",
            description = "Generates a structured file change patch for user review.",
            parametersSchema = "{ \"path\": \"string\", \"content\": \"string\", \"reason\": \"string\" }",
            requiresApproval = false
        ),
        ToolDefinition(
            name = "apply_approved_patch",
            description = "Applies approved changes to disk after explicit user review.",
            parametersSchema = "{ \"diffItems\": \"array\" }",
            requiresApproval = true
        )
    )

    fun executeTool(
        projectId: String,
        toolName: String,
        arguments: Map<String, Any>
    ): ToolExecutionResult {
        return try {
            when (toolName) {
                "list_project_files" -> {
                    val path = arguments["path"]?.toString() ?: ""
                    val list = fileManager.listProjectFiles(projectId, path)
                    val summary = list.joinToString("\n") {
                        (if (it.isDirectory) "[DIR] " else "[FILE] ") + it.relativePath
                    }
                    ToolExecutionResult(toolName, true, summary.ifEmpty { "(Directory empty)" })
                }
                "read_project_file" -> {
                    val path = arguments["path"]?.toString()
                        ?: return ToolExecutionResult(toolName, false, "Missing parameter 'path'")
                    val content = fileManager.readFileContent(projectId, path)
                    ToolExecutionResult(toolName, true, content)
                }
                "search_project" -> {
                    val query = arguments["query"]?.toString() ?: ""
                    val searchContent = arguments["searchContent"]?.toString()?.toBoolean() ?: false
                    val results = fileManager.searchProject(projectId, query, searchContent)
                    val summary = results.joinToString("\n") { it.relativePath }
                    ToolExecutionResult(toolName, true, summary.ifEmpty { "No matching files found for: $query" })
                }
                else -> {
                    ToolExecutionResult(toolName, false, "Tool '$toolName' is not recognized or requires explicit approval flow.")
                }
            }
        } catch (e: Exception) {
            ToolExecutionResult(toolName, false, "Execution failed: ${e.message}")
        }
    }
}
