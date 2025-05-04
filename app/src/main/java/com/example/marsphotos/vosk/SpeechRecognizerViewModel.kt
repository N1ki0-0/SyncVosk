package com.example.marsphotos.vosk

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class SpeechRecognizerViewModel : ViewModel() {
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages: StateFlow<List<String>> = _availableLanguages

    private val _selectedLanguage = MutableStateFlow("en")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    private var speechRecognizerHelper: SpeechRecognizerHelper? = null

    fun initializeRecognizer(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val modelDir = File(context.filesDir, "model-${_selectedLanguage.value}")
                    Log.d("ModelDebug", "Model directory exists: ${modelDir.exists()}")

                    if (!ModelUtils.verifyModelIntegrity(modelDir)) {
                        ModelUtils.copyModelFromAssets(context, _selectedLanguage.value)
                    }

                    Log.d("ModelDebug", "Model files after copy:")
                    modelDir.listFiles()?.forEach { file ->
                        Log.d("ModelDebug", "- ${file.name} (${file.length()} bytes)")
                        if (file.isDirectory) {
                            file.listFiles()?.forEach { subFile ->
                                Log.d("ModelDebug", "  - ${subFile.name} (${subFile.length()} bytes)")
                            }
                        }
                    }

                    speechRecognizerHelper = SpeechRecognizerHelper(
                        context,
                        _selectedLanguage.value
                    ) { result ->
                        _recognizedText.value = result
                    }
                } catch (e: Exception) {
                    Log.e("ModelError", "Initialization failed", e)
                    _recognizedText.value = "Error: ${e.localizedMessage}"
                }
            }
        }
    }

    fun startListening() {
        speechRecognizerHelper?.startListening()
        _isListening.value = true
    }

    fun stopListening() {
        speechRecognizerHelper?.stopListening()
        _isListening.value = false
    }

    /**
     * Считываем из assets все папки model-*
     */
    fun loadAvailableLanguages(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val langs = context.assets.list("")
                ?.filter { it.startsWith("model-") }
                ?.map { it.removePrefix("model-") }
                ?: emptyList()
            _availableLanguages.value = langs
            // выставляем первый элемент как выбранный, если нужно
            if (langs.isNotEmpty() && _selectedLanguage.value !in langs) {
                _selectedLanguage.value = langs[0]
            }
        }
    }

    /**
     * Копируем из assets модель нужного языка в filesDir и пересоздаём recognizer
     */
    fun switchLanguage(context: Context, languageCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ModelUtils.copyModelFromAssets(context, languageCode)

                withContext(Dispatchers.Main) {
                    _selectedLanguage.value = languageCode
                    speechRecognizerHelper?.switchModel(context, languageCode)
                    if (_isListening.value) {
                        speechRecognizerHelper?.startListening()
                    }
                }
            } catch (e: Exception) {
                _recognizedText.value = "Ошибка загрузки модели: ${e.localizedMessage}"
            }
        }
    }
}