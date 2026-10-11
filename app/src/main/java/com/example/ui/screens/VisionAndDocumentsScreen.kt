package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.LoadedDocumentInfo
import com.example.ui.components.HudFramedCard
import com.example.ui.theme.JarvisOnlineGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisionAndDocumentsScreen(
    loadedDocument: LoadedDocumentInfo?,
    hudSpokenText: String,
    hudDetailedCard: String,
    onCaptureCameraPhoto: () -> Unit,
    onPickGalleryImage: () -> Unit,
    onPickDocumentOrCodeFile: () -> Unit,
    onAnalyzeLoadedTarget: (String) -> Unit,
    onClearLoadedTarget: () -> Unit,
    onDirectCodeAnalysis: (codeSnippet: String, instruction: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var customVisionQuestion by rememberSaveable { mutableStateOf("") }
    var codeOrTextSnippet by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "VISION AI, OCR, PDF & CODE DIAGNOSTICS",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // 1. Optical Input & Document Acquisition
        HudFramedCard(
            title = "OPTICAL SENSOR & FILE ACQUISITION",
            subtitle = "ZERO-PERMISSION PICKER + CAMERA"
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCaptureCameraPhoto,
                    modifier = Modifier.testTag("capture_camera_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CAPTURE PHOTO")
                }

                OutlinedButton(
                    onClick = onPickGalleryImage,
                    modifier = Modifier.testTag("pick_gallery_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SELECT IMAGE / QR")
                }

                OutlinedButton(
                    onClick = onPickDocumentOrCodeFile,
                    modifier = Modifier.testTag("pick_document_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OPEN PDF / TEXT / CODE")
                }
            }

            if (loadedDocument != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = loadedDocument.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${loadedDocument.mimeType} • ${loadedDocument.sizeBytes / 1024} KB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(onClick = onClearLoadedTarget) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CLEAR")
                            }
                        }

                        loadedDocument.qrOrBarcodeValue?.let { qr ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "QR Decoded",
                                    tint = JarvisOnlineGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "QR / Barcode Decoded: $qr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = JarvisOnlineGreen
                                )
                            }
                        }

                        loadedDocument.previewBitmap?.let { bmp ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Loaded Visual Target",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }

                        if (loadedDocument.extractedText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = loadedDocument.extractedText.take(500),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Multimodal AI Actions on Loaded Target
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onAnalyzeLoadedTarget("Describe this scene in detail, recognize all visible objects, and explain any diagrams or charts.")
                                }
                            ) {
                                Text("DESCRIBE SCENE & OBJECTS")
                            }

                            OutlinedButton(
                                onClick = {
                                    onAnalyzeLoadedTarget("Extract all visible text accurately using OCR and summarize key points.")
                                }
                            ) {
                                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("OCR & SUMMARIZE")
                            }

                            OutlinedButton(
                                onClick = {
                                    onAnalyzeLoadedTarget("Translate all visible text in this image/document into English, Hindi, and Nepali.")
                                }
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TRANSLATE TEXT")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customVisionQuestion,
                                onValueChange = { customVisionQuestion = it },
                                label = { Text("Ask a question about this image/PDF/file") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (customVisionQuestion.isNotBlank()) {
                                        onAnalyzeLoadedTarget(customVisionQuestion)
                                        customVisionQuestion = ""
                                    }
                                }
                            ) {
                                Text("ASK AI")
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select an image, capture a camera frame, or open a PDF/text/code document to run Gemini Multimodal Vision, ZXing QR/barcode decoding, OCR extraction, or chart analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Source Code Analyzer, Error Diagnostics & Document Generator
        HudFramedCard(
            title = "SOURCE CODE ANALYZER & DOC GENERATOR",
            subtitle = "DEV DIAGNOSTICS"
        ) {
            OutlinedTextField(
                value = codeOrTextSnippet,
                onValueChange = { codeOrTextSnippet = it },
                label = { Text("Paste source code, stack trace, or document prompt...") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("code_analyzer_input")
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (codeOrTextSnippet.isNotBlank()) {
                            onDirectCodeAnalysis(
                                codeOrTextSnippet,
                                "Explain this source code clearly, identify architectural patterns, and highlight potential edge cases:"
                            )
                        }
                    }
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXPLAIN CODE")
                }

                OutlinedButton(
                    onClick = {
                        if (codeOrTextSnippet.isNotBlank()) {
                            onDirectCodeAnalysis(
                                codeOrTextSnippet,
                                "Diagnose this programming error or stack trace and provide the exact corrected code fix:"
                            )
                        }
                    }
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DIAGNOSE BUG")
                }

                OutlinedButton(
                    onClick = {
                        if (codeOrTextSnippet.isNotBlank()) {
                            onDirectCodeAnalysis(
                                codeOrTextSnippet,
                                "Generate a well-structured technical document and executive summary based on this topic:"
                            )
                        }
                    }
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GENERATE DOC")
                }
            }
        }

        // 3. Latest Vision / Document Analysis Output
        HudFramedCard(
            title = "VISION & DOCUMENT INTELLIGENCE OUTPUT",
            subtitle = "GEMINI MULTIMODAL"
        ) {
            Text(
                text = hudSpokenText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (hudDetailedCard.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hudDetailedCard,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
