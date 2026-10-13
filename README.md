# CodePilot AI — OmniRoute Edition
> Native Android AI Coding Agent Workspace & Repository Engine

CodePilot AI is a mobile-first, native Android software engineering environment that provides a full coding-agent experience comparable to ChatGPT Codex, backed by the **OmniRoute** open-source AI gateway.

---

## 🚀 Key Architectural Highlights

1. **OmniRoute AI Gateway Integration**:
   - OpenAI-compatible API interface (`/v1/models`, `/v1/chat/completions`).
   - Dynamic model discovery from running gateway.
   - Routing presets: `auto/coding` and `auto`, when supported by the installed gateway version.
   - Configurable fallback routing when supported by the selected provider/gateway.
   - Streaming Server-Sent Events (SSE) token delivery with cancellation.

2. **Security & Sandboxing**:
   - **Zip Slip Defense**: Canonical path validation prevents directory traversal (`../`) during archive imports.
   - **Bomb Protection**: Entry count (<5,000) and uncompressed byte threshold (<150MB) limits prevent decompression attacks.
   - **Isolated Storage**: Project writes and edits are kept in app-private storage, with canonical path checks on file operations.
   - **Credential at-rest protection**: Gateway and GitHub credentials are encrypted with an AES-GCM key held by Android Keystore.
   - **Token Sanitization**: Redacted credentials across all diagnostics and logs.

3. **Multi-File Editing & Diff Engine**:
   - Structured multi-file proposal parsing (`<file_change>` tags and markdown code blocks).
   - Unified line-by-line diff generation with additions (+), deletions (-), and context.
   - Automated risk evaluation (Low, Medium, High).
   - Selective per-file approvals and atomic application.
   - Automatic snapshot backup prior to edits with one-tap rollback.

4. **Static Code Review & Health Engine**:
   - Automated repository scan checking for hardcoded secrets, swallowed exceptions, missing READMEs, exported Android components, and test gaps.
   - Severity scoring (Critical, High, Medium, Low) and overall Health Score (0-100).
   - Instant "Auto-Fix with Agent" action for identified findings.
   - Markdown & JSON exportable review reports.

5. **Build & Test System**:
   - `BuildExecutionProvider` contract.
   - On-device local structural & syntax checker for Kotlin, Java, JSON, XML, Python, and JavaScript/TypeScript.
   - Remote Gradle Build Worker connector for full cloud compilation and APK artifact generation.
   - Terminal logs viewer with "Explain Build Error with AI" diagnostic action.

6. **GitHub Integration**:
   - Personal Access Token authentication.
   - Authorized repository browser (public & private).
   - Direct zipball stream cloning into sandbox.
   - Branch inspection and Pull Request preparation.

7. **Multilingual Agent**:
   - Supports prompts in **English**, **Hindi**, and **Hinglish** (e.g. *"Is function me null pointer exception handle karo"*).

---

## 🛠️ OmniRoute Setup & Free-Tier Providers

### 1. Running OmniRoute Gateway
OmniRoute runs locally via Docker or npm:
```bash
# Via npm
npx omniroute

# Or via Docker
docker run -d -p 20128:20128 ghcr.io/diegosouzapw/omniroute:latest
```

The gateway exposes its API on port `20128`.

### 2. Android Connection Setup
- **On Android Emulator**:
  Set the Gateway Base URL in CodePilot AI settings to:
  ```
  http://10.0.2.2:20128/v1
  ```
  *(Android Emulator maps `10.0.2.2` to the host machine's `localhost`)*.

- **On Physical Device (via Wi-Fi)**:
  Find your computer's local IP address (e.g. `192.168.1.105`) and set:
  ```
  http://192.168.1.105:20128/v1
  ```

- **Remote Cloud Deployment**:
  Configure your HTTPS domain (e.g., `https://omniroute.example.com/v1`) with optional Bearer access token.

### 3. Recommended Free & Low-Cost Coding Models
Use the model list returned by your running OmniRoute gateway. Model IDs and free-tier availability change over time, so the app does not claim that an unverified model is free.

Recommended starting route, if supported by your gateway version:
- `auto/coding` (coding-focused routing preset)
- `auto` (automatic routing preset)

**Important:** OmniRoute is a gateway/router, not a guarantee of free or unlimited inference. Free usage depends on the connected provider, account eligibility, quotas, and current terms. The app discovers models from `/v1/models` and treats unknown pricing as unverified.

---

## 📱 Feature Completion Matrix

| Feature | Category | Status | Notes |
| :--- | :--- | :--- | :--- |
| **Home Dashboard** | Core UI | **Implemented** | Active project card, model route badge, quick actions, task history |
| **Projects Library** | Project Mgmt | **Implemented** | List workspaces, file counts, switch active, delete with confirmation |
| **New Project & Template Init** | Project Mgmt | **Implemented** | Compose, Python, React TS, and Kotlin starter scaffolds |
| **Safe ZIP Archive Import** | File System | **Implemented & Tested** | Zip Slip path traversal & bomb attack defense |
| **AI Coding Conversation** | AI Agent | **Implemented** | Multi-turn chat, prompt chips, Hinglish/Hindi support, streaming SSE |
| **Multi-File Change Engine** | AI Agent | **Implemented & Tested** | XML & Markdown change-set parsing, risk level rating |
| **Unified Diff Viewer** | Review | **Implemented & Tested** | Green/Red additions/deletions, hunk lines, selective approval |
| **Atomic Apply & Rollback** | Storage | **Implemented** | Auto backup snapshot before applying, rollback action |
| **File Explorer** | File System | **Implemented** | Breadcrumb navigation, file search, create/delete files & folders |
| **Source Code Editor** | Editor | **Implemented** | Live syntax highlighting (Kotlin, Java, Python, JS, XML), gutter line numbers, save |
| **Static Code Review Engine** | Review | **Implemented** | Health score (0-100), secret detector, empty catch audit, Markdown export |
| **Build & Test Console** | Build System | **Implemented** | Profile selection, structural checker, remote worker connector, log viewer |
| **Model & Gateway Config** | Networking | **Implemented** | Base URL, API key, Test connection, dynamic `/v1/models` discovery, timeout |
| **GitHub Integration** | VCS | **Implemented** | PAT authentication, repository browser, safe clone into sandbox |
| **ZIP Export & Native Share** | Sharing | **Implemented** | Pack into standard `.zip` and share via Android Share Sheet (`FileProvider`) |
| **Settings & Security Audit** | Settings | **Implemented** | Security posture verification, language preference, about section |

---

## 🤖 Build an APK automatically on GitHub

This repository includes `.github/workflows/android-apk.yml`.

1. Upload/push the **contents of this project** to the root of a GitHub repository. Do not upload the ZIP as a single file if you want GitHub Actions to build it.
2. Ensure the default branch is `main` or `master`.
3. Open the repository's **Actions** tab.
4. Select **Android APK CI** and choose **Run workflow**, or push a commit to `main`/`master`.
5. Wait for the **Test and build debug APK** job to finish successfully.
6. Open that workflow run, scroll to **Artifacts**, and download `CodePilot-debug-apk-...`.
7. Extract the downloaded artifact ZIP; it contains the installable debug APK.

The workflow runs unit tests before building. It uploads the APK only when the build succeeds, and keeps build/test artifacts for a limited retention period. A debug APK is for testing and is not a Play Store release.

The workflow installs Gradle directly, so a missing `gradlew`/`gradle-wrapper.jar` does not block this GitHub Actions path. For local Android Studio builds, use a compatible Gradle installation and Android SDK platform 36.

## Build limitations

- The in-app local checker performs basic structural checks; it is **not** a real Android Gradle build and does not generate an APK.
- Full remote builds require a separately deployed, isolated build worker implementing the documented API contract. The current worker client does not send a worker authentication token; keep the worker on a trusted private network and do not expose it directly to the public internet until authentication is implemented.
- No paid Codex API key is required by the app's design, but OmniRoute and its connected providers still require whatever credentials or free-tier access those providers currently require.
