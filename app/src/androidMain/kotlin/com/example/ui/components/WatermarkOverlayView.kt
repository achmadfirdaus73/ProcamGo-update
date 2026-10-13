package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.LiveLocationData
import com.example.watermark.IsometricMapDrawer
import com.example.watermark.WatermarkStyle
import java.util.Locale

@Composable
fun WatermarkOverlayView(
    locationData: LiveLocationData,
    tagLabel: String,
    mapBitmap: Bitmap?,
    watermarkStyle: WatermarkStyle = WatermarkStyle.GRID_TRANSPARENT,
    modifier: Modifier = Modifier
) {
    when (watermarkStyle) {
        WatermarkStyle.GRID_TRANSPARENT -> Design1Overlay(locationData, tagLabel, mapBitmap, modifier)
        WatermarkStyle.ISOMETRIC_CARD -> Design2Overlay(locationData, tagLabel, mapBitmap, modifier)
        WatermarkStyle.COMPACT_CAPSULE -> Design3Overlay(locationData, tagLabel, mapBitmap, modifier)
        WatermarkStyle.FLOATING_TAB_MAP -> Design4Overlay(locationData, tagLabel, mapBitmap, modifier)
        WatermarkStyle.SPLIT_CORNER -> Design5Overlay(locationData, tagLabel, mapBitmap, modifier)
        WatermarkStyle.CLEAN_WHITE -> Design6Overlay(locationData, tagLabel, mapBitmap, modifier)
        WatermarkStyle.VERTICAL_STRIP -> Design7Overlay(locationData, tagLabel, mapBitmap, modifier)
        WatermarkStyle.CRYPTO_SEAL -> Design8Overlay(locationData, tagLabel, mapBitmap, modifier)
    }
}

/**
 * Reusable Composable that renders the real OpenStreetMap tile
 * with an isometric 3D tilt perspective or 2D circular/rounded layout with a red location pin.
 */
@Composable
private fun RealOsmMapCanvas(
    mapBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    isIsometric: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xC4FFFFFF)), // Translucent frosted container
        contentAlignment = Alignment.Center
    ) {
        if (mapBitmap != null && !mapBitmap.isRecycled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val nativeCanvas = drawContext.canvas.nativeCanvas
                val rect = RectF(0f, 0f, size.width, size.height)
                if (isIsometric) {
                    IsometricMapDrawer.drawIsometricMap(nativeCanvas, rect, mapBitmap)
                } else {
                    IsometricMapDrawer.draw2DMap(nativeCanvas, rect, mapBitmap, cornerRadius = 14f)
                }
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF38BDF8)
            )
        }
    }
}

// -------------------------------------------------------------
// DESAIN 1: Klasik Grid Presisi (MAP JADI BACKGROUND KARTU!)
// -------------------------------------------------------------
@Composable
private fun Design1Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x990F172A)) // Translucent navy glass
            .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(18.dp))
    ) {
        // Real OpenStreetMap as card background!
        if (mapBitmap != null && !mapBitmap.isRecycled) {
            Image(
                bitmap = mapBitmap.asImageBitmap(),
                contentDescription = "Peta Background",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.40f
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x8C0F172A))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(130.dp)) {
                Text(
                    text = tag.uppercase(Locale.ROOT),
                    color = Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
                Text(
                    text = loc.formattedTime.ifEmpty { "21:39" },
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 38.sp
                )
                Text(
                    text = loc.formattedDate.ifEmpty { "13 Okt 2026" },
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.2.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("🛡️ ASLI", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(96.dp)
                    .background(Color(0x44FFFFFF))
            )
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                    color = Color(0xFFF8FAFC),
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
                val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
                Text(
                    text = "$latStr, $lonStr",
                    color = Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SIG: #${loc.securityResult.signatureToken} • LIVE ENCRYPTED",
                    color = Color(0xFFCBD5E1),
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DESAIN 2: Kartu Isometrik Terverifikasi (Peta Besar & Teks Besar)
// -------------------------------------------------------------
@Composable
private fun Design2Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x941E293B))
            .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(loc.formattedTime.ifEmpty { "21:39" }, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black, lineHeight = 40.sp)
                Text(loc.formattedDate.ifEmpty { "13 Okt 2026" }, color = Color(0xFFE2E8F0), fontSize = 13.5.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC064E3B))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("🛡️ ASLI TERVERIFIKASI", color = Color(0xFF6EE7B7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // LARGER Real OpenStreetMap Box (3D Isometric)
            RealOsmMapCanvas(
                mapBitmap = mapBitmap,
                modifier = Modifier.size(width = 125.dp, height = 94.dp),
                isIsometric = true
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
            color = Color(0xFFF1F5F9),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        Text(
            text = "$latStr, $lonStr   SIG: #${loc.securityResult.signatureToken}",
            color = Color(0xFFE2E8F0),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------
// DESAIN 3: Kapsul Minimalis Horizontal (Peta & Teks Besar)
// -------------------------------------------------------------
@Composable
private fun Design3Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color(0x941E293B))
            .border(1.2.dp, Color(0x44FFFFFF), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LARGER circle containing REAL OpenStreetMap
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xC4FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (mapBitmap != null && !mapBitmap.isRecycled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    val rect = RectF(0f, 0f, size.width, size.height)
                    IsometricMapDrawer.draw2DMap(nativeCanvas, rect, mapBitmap, isCircle = true)
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF38BDF8))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Text(" • ", color = Color.White, fontSize = 12.sp)
                Text("${loc.formattedTime.ifEmpty { "21:39" }} • ${loc.formattedDate.ifEmpty { "13 Okt 2026" }}", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, Jatirasa, Jatiasih, Kota Bks" },
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
            val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
            Text("$latStr, $lonStr", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xCC0F172A))
                .border(1.5.dp, Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = "OK", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
        }
    }
}

// -------------------------------------------------------------
// DESAIN 4: Peta Melayang Tab di Pojok Kanan Atas (Melayang Bersih di Atas Card)
// -------------------------------------------------------------
@Composable
private fun Design4Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // Floating Top-Right Map Tab (Melayang di atas card pada pojok kanan, tanpa menabrak card)
        RealOsmMapCanvas(
            mapBitmap = mapBitmap,
            modifier = Modifier
                .padding(end = 16.dp)
                .width(140.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.2.dp, Color(0x66FFFFFF), RoundedCornerShape(14.dp)),
            isIsometric = true
        )

        Spacer(modifier = Modifier.height(10.dp)) // Jarak pemisah bersih agar tidak tertabrak card di bawahnya

        // Main Translucent Dark Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x940F172A))
                .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${loc.formattedTime.ifEmpty { "21:39" }} • ${loc.formattedDate.ifEmpty { "13 Okt 2026" }}", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.2.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.5.dp)
                ) {
                    Text("🛡️ ASLI", color = Color(0xFF34D399), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                color = Color(0xFFF1F5F9),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
            val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
            Text(
                text = "$latStr, $lonStr   SIG: #${loc.securityResult.signatureToken}",
                color = Color(0xFF38BDF8),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


// -------------------------------------------------------------
// DESAIN 5: Pecah Tiga (Peta Sudut Besar & Teks Besar)
// -------------------------------------------------------------
@Composable
private fun Design5Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Bottom-Left Address Box
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x941E293B))
                    .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                    color = Color(0xFFF8FAFC),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
                val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
                Text("$latStr, $lonStr", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }

            // BIGGER Bottom-Right Real OpenStreetMap Box
            RealOsmMapCanvas(
                mapBitmap = mapBitmap,
                modifier = Modifier.size(92.dp),
                isIsometric = true
            )
        }
    }
}

// -------------------------------------------------------------
// DESAIN 6: Kartu Putih Elegan (Peta & Teks Besar)
// -------------------------------------------------------------
@Composable
private fun Design6Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xB3FFFFFF))
            .border(1.2.dp, Color(0x33000000), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RealOsmMapCanvas(
            mapBitmap = mapBitmap,
            modifier = Modifier.size(width = 108.dp, height = 90.dp),
            isIsometric = true
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tag.uppercase(Locale.ROOT), color = Color(0xFF1D4ED8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(loc.formattedTime.ifEmpty { "21:39" }, color = Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Text(loc.formattedDate.ifEmpty { "13 Okt 2026" }, color = Color(0xFF475569), fontSize = 11.5.sp)
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                color = Color(0xFF1E293B),
                fontSize = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("✓ Asli terverifikasi", color = Color(0xFF059669), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("#${loc.securityResult.signatureToken}", color = Color(0xFF475569), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// -------------------------------------------------------------
// DESAIN 7: Strip Vertikal Kiri (Peta & Nomor Jam Besar)
// -------------------------------------------------------------
@Composable
private fun Design7Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Vertical Strip (Widened & Enlarged)
        Column(
            modifier = Modifier
                .width(82.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x941E293B))
                .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(18.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RealOsmMapCanvas(
                mapBitmap = mapBitmap,
                modifier = Modifier.size(58.dp),
                isIsometric = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            val parts = loc.formattedTime.ifEmpty { "21:39" }.split(":")
            Text(if (parts.isNotEmpty()) parts[0] else "21", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(if (parts.size > 1) parts[1] else "39", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("13 Okt", color = Color(0xFFCBD5E1), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Icon(Icons.Default.Shield, contentDescription = "OK", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
        }

        // Right Info Box
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x941E293B))
                .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                color = Color(0xFFF8FAFC),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
            val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
            Text("$latStr, $lonStr", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text("SIG: #${loc.securityResult.signatureToken} • LIVE ENCRYPTED", color = Color(0xFFCBD5E1), fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// -------------------------------------------------------------
// DESAIN 8: Segel Kriptografis (Stempel & Teks Besar)
// -------------------------------------------------------------
@Composable
private fun Design8Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ENLARGED Circular Crypto Seal
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color(0xCC0F172A))
                .border(2.2.dp, Color(0xFF10B981), CircleShape)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xC4FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (mapBitmap != null && !mapBitmap.isRecycled) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val nativeCanvas = drawContext.canvas.nativeCanvas
                        val rect = RectF(0f, 0f, size.width, size.height)
                        IsometricMapDrawer.draw2DMap(nativeCanvas, rect, mapBitmap, isCircle = true)
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF38BDF8))
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Info Card
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x941E293B))
                .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(loc.formattedTime.ifEmpty { "21:39" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Text(loc.formattedDate.ifEmpty { "13 Okt 2026" }, color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, Jatirasa, Jatiasih, Kota Bks" },
                color = Color(0xFFF8FAFC),
                fontSize = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
            val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
            Text("$latStr, $lonStr   #${loc.securityResult.signatureToken}", color = Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
