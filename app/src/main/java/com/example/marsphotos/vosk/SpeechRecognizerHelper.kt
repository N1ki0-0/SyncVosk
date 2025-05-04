package com.example.marsphotos.vosk

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.IOException

class SpeechRecognizerHelper(
    private val context: Context,
    languageCode: String,
    private val onResult: (String) -> Unit
) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    init {
        initializeModel(languageCode)
    }

    private fun initializeModel(languageCode: String) {
        val modelPath = File(context.filesDir, "model-$languageCode")
        Log.d("ModelCheck", "Model path: ${modelPath.absolutePath}")
        Log.d("ModelCheck", "Model files: ${modelPath.listFiles()?.joinToString { it.name }}")

        // Add explicit check for critical files
        val requiredFiles = listOf("am/final.mdl", "graph/HCLr.fst", "graph/Gr.fst")
        requiredFiles.forEach { file ->
            if (!File(modelPath, file).exists()) {
                throw IOException("Missing required file: $file")
            }
        }

        model = Model(modelPath.absolutePath)
        recognizer = Recognizer(model, 16000.0f)
    }

    fun startListening() {
        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            audioRecord?.startRecording()
        } else {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            return
        }


        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(4096)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    if (recognizer?.acceptWaveForm(buffer, read) == true) {
                        val result = recognizer?.result
                        result?.let {
                            val text = JSONObject(it).optString("text", "")
                            withContext(Dispatchers.Main) {
                                onResult(text)
                            }
                        }
                    }
                }
            }
        }
    }

    fun stopListening() {
        recordingJob?.cancel()
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

    fun switchModel(context: Context, languageCode: String) {
        stopListening()
        recognizer?.close()
        model?.close()
        initializeModel(languageCode)
    }
}