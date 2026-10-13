package com.example.domain.review

import com.example.core.model.FindingSeverity
import com.example.core.model.ProjectReviewReport
import com.example.core.model.ReviewFinding
import com.example.core.storage.ProjectFileManager
import java.io.File
import java.util.UUID

class ProjectReviewEngine(private val fileManager: ProjectFileManager) {

    fun performFullReview(projectId: String, projectName: String): ProjectReviewReport {
        val findings = mutableListOf<ReviewFinding>()
        val files = fileManager.listAllProjectFilesRecursive(projectId)

        // 1. Check for missing README or documentation
        val hasReadme = files.any { it.name.equals("README.md", ignoreCase = true) }
        if (!hasReadme) {
            findings.add(
                ReviewFinding(
                    id = UUID.randomUUID().toString(),
                    title = "Missing Project Documentation",
                    severity = FindingSeverity.MEDIUM,
                    affectedFile = "README.md",
                    explanation = "No README.md found in the root directory. Projects without documentation are harder to onboard and maintain.",
                    suggestedFix = "Create a README.md documenting architecture, build steps, and API contracts.",
                    isVerified = true
                )
            )
        }

        // 2. Scan for hardcoded API keys / tokens in source files
        val secretPatterns = listOf(
            Regex("(AIza[0-9A-Za-z-_]{35})") to "Google/Firebase API Key",
            Regex("ghp_[a-zA-Z0-9]{36}") to "GitHub Personal Access Token",
            Regex("sk-[a-zA-Z0-9]{20,}") to "OpenAI Secret Key",
            Regex("AKIA[0-9A-Z]{16}") to "AWS Access Key"
        )

        for (file in files.filter { !it.isDirectory && it.sizeBytes < 300_000 }) {
            try {
                val content = fileManager.readFileContent(projectId, file.relativePath)
                val lines = content.lines()

                for ((index, line) in lines.withIndex()) {
                    for ((pattern, secretType) in secretPatterns) {
                        val match = pattern.find(line)
                        if (match != null) {
                            findings.add(
                                ReviewFinding(
                                    id = UUID.randomUUID().toString(),
                                    title = "Hardcoded Secret Detected ($secretType)",
                                    severity = FindingSeverity.CRITICAL,
                                    affectedFile = file.relativePath,
                                    lineNumber = index + 1,
                                    codeSnippet = line.trim().take(80),
                                    explanation = "A potentially live secret token was found hardcoded in source control.",
                                    suggestedFix = "Move this token to environment variables or BuildConfig/Secrets Gradle plugin.",
                                    isVerified = true
                                )
                            )
                        }
                    }

                    // Check for empty catch blocks
                    if (line.contains("catch (") && lines.getOrNull(index + 1)?.trim() == "}") {
                        findings.add(
                            ReviewFinding(
                                id = UUID.randomUUID().toString(),
                                title = "Swallowed Exception in Empty Catch Block",
                                severity = FindingSeverity.HIGH,
                                affectedFile = file.relativePath,
                                lineNumber = index + 1,
                                codeSnippet = line.trim(),
                                explanation = "Empty catch block silently suppresses runtime errors, causing difficult-to-trace bugs.",
                                suggestedFix = "Log or rethrow the exception, or provide appropriate fallback error handling.",
                                isVerified = true
                            )
                        )
                    }
                }
            } catch (ignored: Exception) {
            }
        }

        // 3. Test Coverage check
        val testFiles = files.filter { it.name.contains("test", ignoreCase = true) }
        if (testFiles.isEmpty()) {
            findings.add(
                ReviewFinding(
                    id = UUID.randomUUID().toString(),
                    title = "Zero Unit Tests Found",
                    severity = FindingSeverity.HIGH,
                    affectedFile = "src/test",
                    explanation = "No unit test or integration test files detected in the repository.",
                    suggestedFix = "Generate unit tests for core services and viewmodels using CodePilot AI.",
                    isVerified = true
                )
            )
        }

        // 4. Android Manifest inspection (if Android project)
        val manifestFile = files.find { it.name == "AndroidManifest.xml" }
        if (manifestFile != null) {
            try {
                val manifestContent = fileManager.readFileContent(projectId, manifestFile.relativePath)
                if (manifestContent.contains("android:exported=\"true\"") && !manifestContent.contains("<intent-filter>")) {
                    findings.add(
                        ReviewFinding(
                            id = UUID.randomUUID().toString(),
                            title = "Exported Activity Without Intent Filter",
                            severity = FindingSeverity.HIGH,
                            affectedFile = manifestFile.relativePath,
                            explanation = "Exporting components without intent-filters can unintentionally expose app components to third-party apps.",
                            suggestedFix = "Set android:exported=\"false\" unless external app invocation is required.",
                            isVerified = true
                        )
                    )
                }
            } catch (ignored: Exception) {
            }
        }

        // Calculate health score: 100 minus severity deductions
        var score = 100
        for (f in findings) {
            score -= when (f.severity) {
                FindingSeverity.CRITICAL -> 25
                FindingSeverity.HIGH -> 15
                FindingSeverity.MEDIUM -> 8
                FindingSeverity.LOW -> 4
                FindingSeverity.INFO -> 1
            }
        }
        val healthScore = score.coerceIn(15, 100)

        val summary = if (findings.isEmpty()) {
            "Excellent code health! No critical vulnerabilities, swallowed exceptions, or missing test suites detected."
        } else {
            "Found ${findings.size} issues across repository (${findings.count { it.severity == FindingSeverity.CRITICAL }} Critical, ${findings.count { it.severity == FindingSeverity.HIGH }} High, ${findings.count { it.severity == FindingSeverity.MEDIUM }} Medium)."
        }

        return ProjectReviewReport(
            projectId = projectId,
            projectName = projectName,
            generatedAt = System.currentTimeMillis(),
            healthScore = healthScore,
            findings = findings,
            summary = summary
        )
    }

    fun exportReportAsMarkdown(report: ProjectReviewReport): String {
        val sb = StringBuilder()
        sb.append("# CodePilot AI Code Review Report\n\n")
        sb.append("**Project:** ${report.projectName}\n")
        sb.append("**Health Score:** ${report.healthScore}/100\n")
        sb.append("**Summary:** ${report.summary}\n\n")
        sb.append("## Findings Breakdown\n\n")

        for (f in report.findings) {
            sb.append("### [${f.severity}] ${f.title}\n")
            sb.append("- **File:** `${f.affectedFile}` ${if (f.lineNumber != null) "(Line ${f.lineNumber})" else ""}\n")
            sb.append("- **Explanation:** ${f.explanation}\n")
            if (f.codeSnippet.isNotBlank()) {
                sb.append("```\n${f.codeSnippet}\n```\n")
            }
            sb.append("- **Suggested Fix:** ${f.suggestedFix}\n\n")
        }
        return sb.toString()
    }
}
