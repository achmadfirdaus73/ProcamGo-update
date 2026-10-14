package com.example.ui.screens

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.MainViewModel
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.WatermarkOverlayView
import com.example.watermark.WatermarkStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: MainViewModel,
    onNavigateToGallery: () -> Unit
) {
    val locationData by viewModel.locationData.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val capturedResult by viewModel.capturedResult.collectAsState()
    val lensFacing by viewModel.lensFacing.collectAsState()
    val flashMode by viewModel.flashMode.collectAsState()
    val currentMapThumbnail by viewModel.currentMapThumbnail.collectAsState()
    val photoQualityStyle by viewModel.photoQualityStyle.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var noteText by remember { mutableStateOf("") }
    var showTagDialog by remember { mutableStateOf(false) }
    var showStylePicker by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        CameraPreviewView(
            lensFacing = lensFacing,
            flashMode = flashMode,
            onControllerReady = { controller ->
                viewModel.setCameraController(controller)
            },
            modifier = Modifier.fillMaxSize()
        )

        val sec = locationData.securityResult
        val isTampered = !sec.isAuthentic || sec.isMockDetected || sec.isTimeManipulated

        // Top Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.cycleFlashMode() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x660F172A))
                    .testTag("btn_flash_toggle")
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    },
                    contentDescription = "Flash Mode",
                    tint = Color.White
                )
            }

            // Anti-Fake Security Status Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isTampered) Color(0xCC7F1D1D) else Color(0xCC064E3B))
                    .border(1.dp, if (isTampered) Color(0xFFEF4444) else Color(0xFF10B981), RoundedCornerShape(20.dp))
                    .clickable { showSecurityDialog = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("btn_security_status")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTampered) Icons.Default.Warning else Icons.Default.Shield,
                        contentDescription = "Status Keamanan",
                        tint = if (isTampered) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val statusText = when {
                        sec.isMockDetected && sec.isTimeManipulated -> "⚠️ FAKE GPS & WAKTU"
                        sec.isTimeManipulated -> "⚠️ WAKTU MANUAL (PALSU)"
                        sec.isMockDetected -> "⚠️ GPS PALSU"
                        else -> "🛡️ ASLI"
                    }
                    Text(
                        text = statusText,
                        color = if (isTampered) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quality Style Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x880F172A))
                    .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(20.dp))
                    .clickable { viewModel.cyclePhotoQualityStyle() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("btn_quality_style")
            ) {
                Text(
                    text = photoQualityStyle.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { viewModel.toggleLensFacing() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x660F172A))
                    .testTag("btn_switch_camera")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }
        }

        // Prominent Warning Banner if Mock GPS or Time Tampered is detected!
        if (isTampered) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE7F1D1D)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 62.dp, start = 16.dp, end = 16.dp)
                    .clickable { showSecurityDialog = true }
                    .testTag("banner_tampered_warning")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Peringatan", tint = Color(0xFFFCA5A5), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val title = when {
                            sec.isMockDetected && sec.isTimeManipulated -> "PERINGATAN: FAKE GPS & WAKTU MANUAL!"
                            sec.isMockDetected -> "PERINGATAN: FAKE GPS / MOCK LOCATION!"
                            sec.isTimeManipulated -> "PERINGATAN: WAKTU MANUAL TERDETEKSI (PALSU)!"
                            else -> "PERINGATAN: LOKASI ATAU WAKTU TIDAK VALID!"
                        }
                        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        val anomalyText = sec.detectedAnomalies.firstOrNull() ?: "Lokasi atau jam perangkat tidak valid"
                        Text(anomalyText, color = Color(0xFFFECACA), fontSize = 10.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Top-Left Card for Desain 5 (Pecah Tiga / SPLIT_CORNER)
        if (WatermarkStyle.fromString(settings.watermarkStyle) == WatermarkStyle.SPLIT_CORNER) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = if (isTampered) 136.dp else 64.dp, start = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x941E293B))
                    .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                    .padding(14.dp)
                    .align(Alignment.TopStart)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isTampered) Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = if (isTampered) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(settings.companyTag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    val safeTime = locationData.formattedTime.ifEmpty { SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date()) }
                    val safeDate = locationData.formattedDate.ifEmpty { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()) }
                    Text(safeTime, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(safeDate, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                }
            }
        }

        // Bottom Controls with Watermark Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            WatermarkOverlayView(
                locationData = locationData,
                tagLabel = settings.companyTag,
                mapBitmap = currentMapThumbnail,
                watermarkStyle = WatermarkStyle.fromString(settings.watermarkStyle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTagDialog = true }
                    .testTag("live_watermark_card")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button
                IconButton(
                    onClick = onNavigateToGallery,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0x660F172A))
                        .testTag("btn_quick_gallery")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Buka Galeri",
                        tint = Color.White
                    )
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0x44FFFFFF))
                        .border(3.dp, Color.White, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            viewModel.triggerShutter()
                        }
                        .testTag("btn_shutter"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Watermark Style Picker Button (Right of Shutter)
                IconButton(
                    onClick = { showStylePicker = true },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0x660F172A))
                        .border(1.dp, Color(0x6638BDF8), CircleShape)
                        .testTag("btn_watermark_style_picker")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Pilih Gaya Watermark",
                        tint = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // Preview Captured Dialog
        if (capturedResult != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissPreview() },
                title = { Text("Pratinjau Foto Watermark", color = Color.White) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            bitmap = capturedResult!!.bitmap.asImageBitmap(),
                            contentDescription = "Foto Watermark",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Catatan Kunjungan (Opsional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveToGalleryAndLocal(noteText)
                            noteText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.testTag("btn_save_photo")
                    ) {
                        Text("Simpan & Sinkron")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissPreview() },
                        modifier = Modifier.testTag("btn_discard_photo")
                    ) {
                        Text("Ulangi", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Quick Tag Edit Dialog when tapping Watermark Card
        if (showTagDialog) {
            var tempTag by remember { mutableStateOf(settings.companyTag) }
            AlertDialog(
                onDismissRequest = { showTagDialog = false },
                title = { Text("Ubah Label Tag / Unit", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = tempTag,
                        onValueChange = { tempTag = it },
                        label = { Text("Nama Tag / Unit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_quick_tag")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateSettings(settings.copy(companyTag = tempTag.trim().ifEmpty { "KOLEKPRO" }))
                            showTagDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTagDialog = false }) {
                        Text("Batal", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // Watermark Style Selection ModalBottomSheet
        if (showStylePicker) {
            ModalBottomSheet(
                onDismissRequest = { showStylePicker = false },
                containerColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Pilih Desain Kostum Watermark",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        "Pilih salah satu dari 8 template desain watermark sesuai kebutuhan Anda:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val currentStyle = WatermarkStyle.fromString(settings.watermarkStyle)

                    WatermarkStyle.values().forEach { style ->
                        val isSelected = style == currentStyle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF0F172A))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF60A5FA) else Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.updateWatermarkStyle(style)
                                    showStylePicker = false
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = style.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = style.desc,
                                    color = if (isSelected) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                                    fontSize = 11.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Dipilih",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

        // Security Details & Anti-Fake Test Dialog
        if (showSecurityDialog) {
            AlertDialog(
                onDismissRequest = { showSecurityDialog = false },
                containerColor = Color(0xFF1E293B),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isTampered) Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = "Security",
                            tint = if (isTampered) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTampered) "Peringatan Keamanan" else "Keamanan Lokasi & Waktu",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isTampered)
                                "Terdeteksi potensi manipulasi pada perangkat ini:"
                            else
                                "Sistem memvalidasi keaslian GPS dan jam perangkat secara realtime:",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Status GPS Mock
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mock / Fake GPS:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(
                                text = if (sec.isMockDetected) "TERDETEKSI (PALSU)" else "BERSIH (ASLI)",
                                color = if (sec.isMockDetected) Color(0xFFEF4444) else Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Status Jam Perangkat
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Integritas Jam:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(
                                text = if (sec.isTimeManipulated) "MANUAL (PALSU)" else "OTOMATIS (ASLI)",
                                color = if (sec.isTimeManipulated) Color(0xFFEF4444) else Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Token Kriptografis
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Token Kriptografis:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text(
                                text = "#${sec.signatureToken}",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (sec.detectedAnomalies.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Detail Anomali:", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            sec.detectedAnomalies.forEach { anomaly ->
                                Text("• $anomaly", color = Color(0xFFFECACA), fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2238BDF8))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Otomatis",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pemindaian aktif otomatis setiap detik secara real-time.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                },
                confirmButton = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sec.isTimeManipulated) {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_DATE_SETTINGS).apply {
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Atur Jam Otomatis", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TextButton(onClick = { showSecurityDialog = false }) {
                            Text("Tutup", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    }
}
