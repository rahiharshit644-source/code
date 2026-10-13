package com.example.core.storage

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object SafeZipManager {
    private const val MAX_ENTRIES = 5000
    private const val MAX_TOTAL_SIZE = 150 * 1024 * 1024L // 150 MB max uncompressed
    private const val BUFFER_SIZE = 4096

    class SafeZipException(message: String) : Exception(message)

    /**
     * Safely unpacks a ZIP stream into targetDir, protecting against:
     * - Zip Slip / Path Traversal
     * - Decompression bombs (excessive size or entry count)
     */
    fun extractZipSafely(
        zipInputStream: InputStream,
        targetDir: File,
        onProgress: ((entryName: String, extractedCount: Int) -> Unit)? = null
    ): Result<Int> {
        return try {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val canonicalDestDir = targetDir.canonicalFile

            var totalBytesRead = 0L
            var entryCount = 0

            ZipInputStream(BufferedInputStream(zipInputStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    entryCount++
                    if (entryCount > MAX_ENTRIES) {
                        throw SafeZipException("Archive exceeds maximum permitted entries ($MAX_ENTRIES). Operation aborted.")
                    }

                    val destFile = File(canonicalDestDir, entry.name).canonicalFile
                    val destFilePath = destFile.canonicalPath

                    // Zip Slip protection: ensure entry does not escape destination directory
                    if (!destFilePath.startsWith(canonicalDestDir.path + File.separator) && destFilePath != canonicalDestDir.path) {
                        throw SafeZipException("Path traversal attempt detected in entry: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos ->
                            BufferedOutputStream(fos, BUFFER_SIZE).use { bos ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var len: Int
                                while (zis.read(buffer).also { len = it } != -1) {
                                    totalBytesRead += len
                                    if (totalBytesRead > MAX_TOTAL_SIZE) {
                                        throw SafeZipException("Decompression size limit exceeded (>150MB). Potential zip bomb.")
                                    }
                                    bos.write(buffer, 0, len)
                                }
                            }
                        }
                    }

                    onProgress?.invoke(entry.name, entryCount)
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(entryCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compresses sourceDir into targetZipFile recursively.
     */
    fun createZipFromDirectory(sourceDir: File, targetZipFile: File): Result<File> {
        return try {
            if (!sourceDir.exists()) {
                return Result.failure(IllegalArgumentException("Source directory does not exist: ${sourceDir.path}"))
            }
            targetZipFile.parentFile?.mkdirs()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZipFile))).use { zos ->
                val sourceCanonical = sourceDir.canonicalFile
                compressDirectoryRecursive(sourceCanonical, sourceCanonical, zos)
            }
            Result.success(targetZipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressDirectoryRecursive(
        rootDir: File,
        currentDir: File,
        zos: ZipOutputStream
    ) {
        val files = currentDir.listFiles() ?: return
        val buffer = ByteArray(BUFFER_SIZE)

        for (file in files) {
            // Ignore hidden git/gradle caches inside zip if desired, or include all relevant project files
            if (file.name == ".gradle" || file.name == "build") {
                continue
            }

            val relativePath = rootDir.toURI().relativize(file.toURI()).path

            if (file.isDirectory) {
                val entryName = if (relativePath.endsWith("/")) relativePath else "$relativePath/"
                val zipEntry = ZipEntry(entryName)
                zos.putNextEntry(zipEntry)
                zos.closeEntry()
                compressDirectoryRecursive(rootDir, file, zos)
            } else {
                val zipEntry = ZipEntry(relativePath)
                zos.putNextEntry(zipEntry)
                FileInputStream(file).use { fis ->
                    BufferedInputStream(fis).use { bis ->
                        var len: Int
                        while (bis.read(buffer).also { len = it } != -1) {
                            zos.write(buffer, 0, len)
                        }
                    }
                }
                zos.closeEntry()
            }
        }
    }
}
