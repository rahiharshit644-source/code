package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.*
import com.example.core.network.ConnectionDiagnostic
import com.example.core.network.GitHubClient
import com.example.core.network.OmniRouteClient
import com.example.core.storage.*
import com.example.domain.agent.CodingAgentEngine
import com.example.domain.agent.ProjectContextIndexer
import com.example.domain.build.BuildExecutionProvider
import com.example.domain.build.LocalStaticChecker
import com.example.domain.build.RemoteBuildWorkerProvider
import com.example.domain.review.ProjectReviewEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

enum class AppScreen {
    DASHBOARD,
    PROJECTS_LIBRARY,
    NEW_PROJECT,
    CHAT,
    FILE_EXPLORER,
    SOURCE_EDITOR,
    DIFF_REVIEW,
    PROJECT_REVIEW,
    BUILD_CONSOLE,
    TASK_HISTORY,
    GATEWAY_CONFIG,
    GITHUB,
    EXPORT,
    SETTINGS
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val database = AppDatabase.getDatabase(context)
    val projectDao = database.projectDao()
    val taskDao = database.taskDao()

    val fileManager = ProjectFileManager(context)
    val preferencesManager = PreferencesManager(context)
    val omniRouteClient = OmniRouteClient()
    val gitHubClient = GitHubClient()

    private val contextIndexer = ProjectContextIndexer(fileManager)
    val agentEngine = CodingAgentEngine(omniRouteClient, fileManager, contextIndexer)
    val reviewEngine = ProjectReviewEngine(fileManager)

    // Navigation Backstack
    private val _screenStack = MutableStateFlow(listOf(AppScreen.DASHBOARD))
    val currentScreen: StateFlow<AppScreen> = _screenStack.map { it.lastOrNull() ?: AppScreen.DASHBOARD }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppScreen.DASHBOARD)

    fun navigateTo(screen: AppScreen) {
        val current = _screenStack.value
        if (current.lastOrNull() != screen) {
            _screenStack.value = current + screen
        }
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        if (current.size > 1) {
            _screenStack.value = current.dropLast(1)
            return true
        }
        return false
    }

    // Projects State
    val projects: StateFlow<List<ProjectEntity>> = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    // Config & Preferences State
    val omniRouteConfig: StateFlow<OmniRouteConfig> = preferencesManager.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, OmniRouteConfig())

    val githubToken: StateFlow<String> = preferencesManager.githubTokenFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val remoteWorkerUrl: StateFlow<String> = preferencesManager.remoteWorkerUrlFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val themeMode: StateFlow<String> = preferencesManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "dark")

    // Gateway & Models State
    private val _connectionDiagnostic = MutableStateFlow<ConnectionDiagnostic?>(null)
    val connectionDiagnostic: StateFlow<ConnectionDiagnostic?> = _connectionDiagnostic.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _availableModels = MutableStateFlow<List<ModelOption>>(omniRouteClient.getFallbackModelPresets())
    val availableModels: StateFlow<List<ModelOption>> = _availableModels.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    // Chat / Coding Agent State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAgentBusy = MutableStateFlow(false)
    val isAgentBusy: StateFlow<Boolean> = _isAgentBusy.asStateFlow()

    // Diff / ChangeSet State
    private val _pendingChangeSet = MutableStateFlow<ProposedChangeSet?>(null)
    val pendingChangeSet: StateFlow<ProposedChangeSet?> = _pendingChangeSet.asStateFlow()

    // File Explorer & Editor State
    private val _currentExplorerPath = MutableStateFlow("")
    val currentExplorerPath: StateFlow<String> = _currentExplorerPath.asStateFlow()

    private val _projectFiles = MutableStateFlow<List<ProjectFile>>(emptyList())
    val projectFiles: StateFlow<List<ProjectFile>> = _projectFiles.asStateFlow()

    private val _selectedFile = MutableStateFlow<ProjectFile?>(null)
    val selectedFile: StateFlow<ProjectFile?> = _selectedFile.asStateFlow()

    private val _editorContent = MutableStateFlow("")
    val editorContent: StateFlow<String> = _editorContent.asStateFlow()

    private val _isEditorDirty = MutableStateFlow(false)
    val isEditorDirty: StateFlow<Boolean> = _isEditorDirty.asStateFlow()

    // Project Review State
    private val _reviewReport = MutableStateFlow<ProjectReviewReport?>(null)
    val reviewReport: StateFlow<ProjectReviewReport?> = _reviewReport.asStateFlow()

    private val _isReviewing = MutableStateFlow(false)
    val isReviewing: StateFlow<Boolean> = _isReviewing.asStateFlow()

    // Build State
    private val _buildResult = MutableStateFlow<BuildResult?>(null)
    val buildResult: StateFlow<BuildResult?> = _buildResult.asStateFlow()

    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()

    // GitHub State
    private val _githubRepos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val githubRepos: StateFlow<List<GitHubRepo>> = _githubRepos.asStateFlow()

    private val _isLoadingGithub = MutableStateFlow(false)
    val isLoadingGithub: StateFlow<Boolean> = _isLoadingGithub.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // Auto-select initial active project or create starter demo project
        viewModelScope.launch {
            preferencesManager.activeProjectIdFlow.collect { savedId ->
                if (savedId != null) {
                    val p = projectDao.getProjectById(savedId)
                    if (p != null) {
                        selectProject(p)
                    }
                }
            }
        }

        viewModelScope.launch {
            val existing = projectDao.getAllProjects().first()
            if (existing.isEmpty()) {
                createProject("CodePilot Demo", "Jetpack Compose sample workspace", "compose")
            } else if (_activeProject.value == null) {
                selectProject(existing.first())
            }
        }
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun clearStatus() {
        _statusMessage.value = null
    }

    fun selectProject(project: ProjectEntity) {
        val previousProjectId = _activeProject.value?.id
        if (previousProjectId != null && previousProjectId != project.id) {
            // Never allow a pending diff from one project to be applied to another.
            _pendingChangeSet.value = null
            _chatMessages.value = emptyList()
            _selectedFile.value = null
            _editorContent.value = ""
            _isEditorDirty.value = false
            _currentExplorerPath.value = ""
            _projectFiles.value = emptyList()
        }
        _activeProject.value = project
        viewModelScope.launch {
            preferencesManager.updateActiveProjectId(project.id)
            refreshProjectFiles()
        }
    }

    fun createProject(name: String, description: String, template: String) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            fileManager.initProjectTemplate(id, template, name)
            val files = fileManager.listAllProjectFilesRecursive(id)

            val entity = ProjectEntity(
                id = id,
                name = name,
                description = description,
                rootPath = id,
                primaryLanguage = when (template.lowercase()) {
                    "python" -> "Python"
                    "react", "typescript" -> "TypeScript"
                    else -> "Kotlin"
                },
                fileCount = files.count { !it.isDirectory }
            )
            projectDao.insertProject(entity)
            selectProject(entity)
            showStatus("Project '$name' created successfully!")
            navigateTo(AppScreen.DASHBOARD)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectDao.deleteProject(project)
            taskDao.deleteTasksForProject(project.id)
            fileManager.deleteProjectFiles(project.id)
            if (_activeProject.value?.id == project.id) {
                val remaining = projectDao.getAllProjects().first()
                if (remaining.isNotEmpty()) {
                    selectProject(remaining.first())
                } else {
                    _activeProject.value = null
                }
            }
            showStatus("Project '${project.name}' deleted.")
        }
    }

    fun importZipArchive(inputStream: InputStream, projectName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val targetDir = fileManager.getProjectDir(id)

            val extractResult = SafeZipManager.extractZipSafely(inputStream, targetDir)
            if (extractResult.isSuccess) {
                val files = fileManager.listAllProjectFilesRecursive(id)
                val entity = ProjectEntity(
                    id = id,
                    name = projectName,
                    description = "Imported from ZIP archive",
                    rootPath = id,
                    primaryLanguage = "Kotlin",
                    fileCount = files.count { !it.isDirectory }
                )
                projectDao.insertProject(entity)
                selectProject(entity)
                showStatus("Extracted ${extractResult.getOrNull()} files safely into '$projectName'")
                withContext(Dispatchers.Main) {
                    navigateTo(AppScreen.FILE_EXPLORER)
                }
            } else {
                fileManager.deleteProjectFiles(id)
                showStatus("ZIP import failed: ${extractResult.exceptionOrNull()?.message}")
            }
        }
    }

    fun refreshProjectFiles() {
        val proj = _activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val list = fileManager.listProjectFiles(proj.id, _currentExplorerPath.value)
            _projectFiles.value = list
        }
    }

    fun navigateExplorerTo(relativePath: String) {
        _currentExplorerPath.value = relativePath
        refreshProjectFiles()
    }

    fun openFileInEditor(file: ProjectFile) {
        val proj = _activeProject.value ?: return
        if (file.isDirectory) {
            navigateExplorerTo(file.relativePath)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = fileManager.readFileContent(proj.id, file.relativePath)
                _selectedFile.value = file
                _editorContent.value = text
                _isEditorDirty.value = false
                withContext(Dispatchers.Main) {
                    navigateTo(AppScreen.SOURCE_EDITOR)
                }
            } catch (e: Exception) {
                showStatus("Error opening file: ${e.message}")
            }
        }
    }

    fun updateEditorContent(newContent: String) {
        _editorContent.value = newContent
        _isEditorDirty.value = true
    }

    fun saveEditorContent() {
        val proj = _activeProject.value ?: return
        val file = _selectedFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fileManager.writeFileContent(proj.id, file.relativePath, _editorContent.value)
                _isEditorDirty.value = false
                showStatus("Saved ${file.name}")
                refreshProjectFiles()
            } catch (e: Exception) {
                showStatus("Failed to save: ${e.message}")
            }
        }
    }

    fun createNewFileOrFolder(name: String, isFolder: Boolean) {
        val proj = _activeProject.value ?: return
        val currentDir = _currentExplorerPath.value
        val relPath = if (currentDir.isEmpty()) name else "$currentDir/$name"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isFolder) {
                    fileManager.createDirectory(proj.id, relPath)
                } else {
                    fileManager.writeFileContent(proj.id, relPath, "")
                }
                refreshProjectFiles()
                showStatus("Created $name")
            } catch (e: Exception) {
                showStatus("Error: ${e.message}")
            }
        }
    }

    fun deleteFileFromExplorer(file: ProjectFile) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ok = fileManager.deleteFile(proj.id, file.relativePath)
            if (ok) {
                showStatus("Deleted ${file.name}")
                refreshProjectFiles()
            } else {
                showStatus("Failed to delete ${file.name}")
            }
        }
    }

    // AI Coding Agent Execution
    fun sendAgentPrompt(prompt: String) {
        val proj = _activeProject.value ?: run {
            showStatus("Please select or create an active project first.")
            return
        }
        val config = omniRouteConfig.value

        val historyForTask = _chatMessages.value.toList()
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = prompt
        )
        val initialAgentMsg = ChatMessage(
            sender = MessageSender.AGENT,
            text = "CodePilot is analyzing codebase...",
            isStreaming = true,
            modelBadge = config.selectedModel
        )

        _chatMessages.value = _chatMessages.value + userMsg + initialAgentMsg
        _isAgentBusy.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val taskId = UUID.randomUUID().toString()
            val fullResponse = StringBuilder()

            try {
                agentEngine.executeCodingTask(proj.id, prompt, config, historyForTask).collect { chunk ->
                    fullResponse.append(chunk)
                    // Update streaming message in UI
                    _chatMessages.value = _chatMessages.value.dropLast(1) + initialAgentMsg.copy(
                        text = fullResponse.toString(),
                        isStreaming = true
                    )
                }

                // Parse structured change-set from response
                val changeSet = agentEngine.parseOutputToChangeSet(
                    taskId = taskId,
                    projectId = proj.id,
                    title = prompt.take(40),
                    modelOutput = fullResponse.toString()
                )

                if (changeSet.files.isNotEmpty()) {
                    _pendingChangeSet.value = changeSet
                }

                // Finalize chat message
                _chatMessages.value = _chatMessages.value.dropLast(1) + initialAgentMsg.copy(
                    text = fullResponse.toString(),
                    isStreaming = false,
                    associatedChangeSet = if (changeSet.files.isNotEmpty()) changeSet else null
                )

                // Save task record to Room
                val taskEntity = AgentTaskEntity(
                    id = taskId,
                    projectId = proj.id,
                    prompt = prompt,
                    modelUsed = config.selectedModel,
                    status = if (changeSet.files.isNotEmpty()) "PROPOSED" else "COMPLETED",
                    summary = "Proposed ${changeSet.files.size} changes",
                    changedFilesJson = changeSet.files.map { it.filePath }.toString()
                )
                taskDao.insertTask(taskEntity)

            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value.dropLast(1) + initialAgentMsg.copy(
                    text = "Agent error: ${e.message}",
                    isStreaming = false
                )
            } finally {
                _isAgentBusy.value = false
            }
        }
    }

    fun applyPendingChangeSet() {
        val proj = _activeProject.value ?: return
        val changeSet = _pendingChangeSet.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val result = fileManager.applyChangeSet(proj.id, changeSet.files)
            if (result.isSuccess) {
                val backupId = result.getOrNull()
                val savedTask = taskDao.getTaskById(changeSet.taskId)
                if (savedTask != null && backupId != null) {
                    taskDao.updateTask(
                        savedTask.copy(
                            status = "APPLIED",
                            summary = "Applied ${changeSet.files.count { it.isApproved }} approved changes",
                            rollbackBackupId = backupId
                        )
                    )
                }
                showStatus("Applied ${changeSet.files.count { it.isApproved }} approved changes safely! Backup created: $backupId")
                _pendingChangeSet.value = null
                refreshProjectFiles()
                withContext(Dispatchers.Main) {
                    navigateTo(AppScreen.FILE_EXPLORER)
                }
            } else {
                showStatus("Failed to apply changes: ${result.exceptionOrNull()?.message}")
            }
        }
    }


    fun rollbackTask(task: AgentTaskEntity) {
        val backupId = task.rollbackBackupId ?: run {
            showStatus("No rollback checkpoint is available for this task.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val restored = runCatching { fileManager.restoreBackup(task.projectId, backupId) }.getOrDefault(false)
            if (restored) {
                taskDao.updateTask(task.copy(status = "ROLLED_BACK"))
                showStatus("Restored project checkpoint for task ${task.id}.")
                if (_activeProject.value?.id == task.projectId) refreshProjectFiles()
            } else {
                showStatus("Could not restore the saved checkpoint. It may have been removed.")
            }
        }
    }

    fun rejectPendingChangeSet() {
        _pendingChangeSet.value = null
        showStatus("Change set rejected.")
    }

    fun toggleFileApproval(fileId: String) {
        val current = _pendingChangeSet.value ?: return
        val updated = current.files.map {
            if (it.id == fileId) it.copy(isApproved = !it.isApproved) else it
        }
        _pendingChangeSet.value = current.copy(files = updated)
    }

    // Static Code Review
    fun runProjectReview() {
        val proj = _activeProject.value ?: return
        _isReviewing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val report = reviewEngine.performFullReview(proj.id, proj.name)
            _reviewReport.value = report
            _isReviewing.value = false
            withContext(Dispatchers.Main) {
                navigateTo(AppScreen.PROJECT_REVIEW)
            }
        }
    }

    // Build Execution
    fun runBuild(profile: String = "Debug Build") {
        val proj = _activeProject.value ?: return
        _isBuilding.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val checker = LocalStaticChecker(fileManager)
            val provider: BuildExecutionProvider = if (remoteWorkerUrl.value.isNotBlank()) {
                RemoteBuildWorkerProvider(remoteWorkerUrl.value, checker)
            } else {
                checker
            }
            val res = provider.executeBuild(proj.id, profile)
            _buildResult.value = res
            _isBuilding.value = false
            withContext(Dispatchers.Main) {
                navigateTo(AppScreen.BUILD_CONSOLE)
            }
        }
    }

    // Gateway & Settings
    fun testGatewayConnection() {
        _isTestingConnection.value = true
        viewModelScope.launch {
            val diag = omniRouteClient.testConnection(omniRouteConfig.value)
            _connectionDiagnostic.value = diag
            _isTestingConnection.value = false
        }
    }

    fun discoverModels() {
        _isLoadingModels.value = true
        viewModelScope.launch {
            val res = omniRouteClient.fetchModels(omniRouteConfig.value)
            if (res.isSuccess) {
                _availableModels.value = res.getOrDefault(omniRouteClient.getFallbackModelPresets())
                showStatus("Discovered ${_availableModels.value.size} models from OmniRoute!")
            } else {
                showStatus("Could not fetch models: ${res.exceptionOrNull()?.message}. Using verified presets.")
            }
            _isLoadingModels.value = false
        }
    }

    fun updateConfig(newConfig: OmniRouteConfig) {
        viewModelScope.launch {
            preferencesManager.updateConfig(newConfig)
            showStatus("Gateway settings saved.")
        }
    }

    fun clearCredentials() {
        viewModelScope.launch {
            preferencesManager.clearCredentials()
            showStatus("Saved gateway and GitHub credentials were cleared.")
        }
    }

    // Export & Sharing
    fun exportProjectAsZip(): File? {
        val proj = _activeProject.value ?: return null
        val projDir = fileManager.getProjectDir(proj.id)
        val safeName = proj.name.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_').ifBlank { "project" }
        val exportFile = File(context.cacheDir, "${safeName}_export.zip")
        val res = SafeZipManager.createZipFromDirectory(projDir, exportFile)
        return if (res.isSuccess) exportFile else null
    }

    fun shareProjectZip() {
        val file = exportProjectAsZip() ?: run {
            showStatus("Failed to generate project archive for sharing.")
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Exported project: ${_activeProject.value?.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Project via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // GitHub Integration
    fun connectGitHub(token: String) {
        viewModelScope.launch {
            preferencesManager.updateGithubToken(token)
            fetchGitHubRepos(token)
        }
    }

    fun fetchGitHubRepos(token: String = githubToken.value) {
        if (token.isBlank()) return
        _isLoadingGithub.value = true
        viewModelScope.launch {
            val res = gitHubClient.getRepositories(token)
            if (res.isSuccess) {
                _githubRepos.value = res.getOrDefault(emptyList())
            } else {
                showStatus("GitHub error: ${res.exceptionOrNull()?.message}")
            }
            _isLoadingGithub.value = false
        }
    }

    fun importGitHubRepo(repo: GitHubRepo) {
        val token = githubToken.value
        if (token.isBlank()) {
            showStatus("Please enter your GitHub Personal Access Token first.")
            return
        }
        _isLoadingGithub.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val streamRes = gitHubClient.downloadRepoZipStream(token, repo.fullName, repo.defaultBranch)
            if (streamRes.isSuccess) {
                importZipArchive(streamRes.getOrThrow(), repo.name)
            } else {
                showStatus("Failed to import GitHub repo: ${streamRes.exceptionOrNull()?.message}")
            }
            _isLoadingGithub.value = false
        }
    }
}
