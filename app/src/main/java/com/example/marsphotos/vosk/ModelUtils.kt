package com.example.marsphotos.vosk

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import java.io.File
import java.io.IOException

object ModelUtils {
    fun getAvailableLanguages(context: Context): List<String> {
        val filesDir = context.filesDir
        return filesDir.listFiles { file -> file.isDirectory && file.name.startsWith("model-") }
            ?.map { it.name.removePrefix("model-") } ?: emptyList()
    }

    fun copyModelFromAssets(context: Context, languageCode: String) {
        val assetManager = context.assets
        val modelDir = "model-$languageCode"
        val targetDir = File(context.filesDir, modelDir)

        if (!isModelValid(targetDir)) {
            targetDir.deleteRecursively()
            targetDir.mkdirs()
            copyAssetsRecursively(assetManager, modelDir, File(context.filesDir, modelDir))

            if (!isModelValid(targetDir)) {
                throw IOException("Model copy verification failed")
            }
        }
    }

    private fun isModelValid(modelDir: File): Boolean {
        val requiredFiles = listOf(
            "am/final.mdl",
            "graph/HCLr.fst",
            "graph/Gr.fst",
            "ivector/final.ie"
        )
        return requiredFiles.all { File(modelDir, it).exists() }
    }

    private fun copyAssetsRecursively(
        assetManager: AssetManager,
        assetPath: String,
        targetDir: File
    ) {
        // Узнаём список «дочерних» элементов в assets/assetPath
        val list = assetManager.list(assetPath)

        if (list == null || list.isEmpty()) {
            // Это файл, запишем его прямо в targetDir
            // убедимся, что parent-папка существует
            targetDir.parentFile?.mkdirs()
            assetManager.open(assetPath).use { input ->
                File(targetDir.parentFile, targetDir.name).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            // Это директория — создаём её и рекурсивно проходим внутрь
            if (!targetDir.exists()) targetDir.mkdirs()
            list.forEach { child ->
                copyAssetsRecursively(
                    assetManager,
                    // assetPath + "/" + имя_файла_или_папки
                    if (assetPath.isEmpty()) child else "$assetPath/$child",
                    // в targetDir создаём файл или папку с тем же именем
                    File(targetDir, child)
                )
            }
        }
    }

    fun verifyModelIntegrity(modelDir: File): Boolean {
        val requiredFiles = listOf(
            File(modelDir, "am/final.mdl"),
            File(modelDir, "graph/HCLr.fst"),
            File(modelDir, "graph/Gr.fst"),
            File(modelDir, "ivector/final.ie"),
            File(modelDir, "conf/mfcc.conf")
        )

        requiredFiles.forEach { file ->
            if (!file.exists()) {
                Log.e("ModelCheck", "Missing file: ${file.absolutePath}")
                return false
            }
            if (file.length() == 0L) {
                Log.e("ModelCheck", "Empty file: ${file.absolutePath}")
                return false
            }
        }
        return true
    }
}