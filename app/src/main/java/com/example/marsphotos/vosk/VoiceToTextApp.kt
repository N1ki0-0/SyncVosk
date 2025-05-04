package com.example.marsphotos.vosk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun VoiceToTextApp(viewModel: SpeechRecognizerViewModel) {
    val recognizedText by viewModel.recognizedText.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Выбранный язык: $selectedLanguage")
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            Button(onClick = { expanded = true }) {
                Text("Выбрать язык")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableLanguages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language) },
                        onClick = {
                            viewModel.switchLanguage(context, language)

                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (isListening) {
                viewModel.stopListening()
            } else {
                viewModel.startListening()
            }
        }) {
            Text(text = if (isListening) "Стоп" else "Говорить")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = recognizedText)
    }
}