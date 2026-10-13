package com.example.core.network

import android.util.Base64
import com.example.core.model.GitHubRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.concurrent.TimeUnit

class GitHubClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getRepositories(token: String): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/user/repos?sort=updated&per_page=30")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github.v3+json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("GitHub API error HTTP ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                val list = mutableListOf<GitHubRepo>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        GitHubRepo(
                            id = obj.getLong("id"),
                            name = obj.getString("name"),
                            fullName = obj.getString("full_name"),
                            description = obj.optString("description", null),
                            defaultBranch = obj.optString("default_branch", "main"),
                            isPrivate = obj.getBoolean("private"),
                            htmlUrl = obj.getString("html_url")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBranches(token: String, repoFullName: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repoFullName/branches")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github.v3+json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to fetch branches: HTTP ${response.code}"))
                }
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getJSONObject(i).getString("name"))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadRepoZipStream(
        token: String,
        repoFullName: String,
        ref: String = "main"
    ): Result<InputStream> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repoFullName/zipball/$ref")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github.v3+json")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return@withContext Result.failure(Exception("Failed to download repository archive (HTTP ${response.code})"))
            }
            val stream = response.body?.byteStream()
                ?: return@withContext Result.failure(Exception("Empty response body stream"))
            Result.success(stream)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPullRequest(
        token: String,
        repoFullName: String,
        title: String,
        headBranch: String,
        baseBranch: String,
        bodyText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("title", title)
            put("head", headBranch)
            put("base", baseBranch)
            put("body", bodyText)
        }
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repoFullName/pulls")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github.v3+json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val obj = JSONObject(body)
                    Result.success(obj.getString("html_url"))
                } else {
                    Result.failure(Exception("PR creation failed: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
