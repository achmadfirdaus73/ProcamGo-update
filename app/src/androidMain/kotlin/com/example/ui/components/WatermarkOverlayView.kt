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
import androidx.compose.material.icons.filled.Warning
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
import com.example.security.SecurityCheckResult
import com.example.watermark.IsometricMapDrawer
import com.example.watermark.WatermarkStyle
import java.text.SimpleDateFormat
import java.util.Date
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
 * Reusable dynamic Security Badge for Anti-Fake GPS & Tamper Detection.
 * Displays vibrant green when authentic, and striking red when mock GPS or manual time manipulation is detected.
 */
@Composable
fun SecurityBadge(
    sec: SecurityCheckResult,
    modifier: Modifier = Modifier,
    isFilled: Boolean = false
) {
    val isTampered = !sec.isAuthentic || sec.isMockDetected || sec.isTimeManipulated
    val label = when {
        sec.isMockDetected && sec.isTimeManipulated -> "⚠️ FAKE GPS & WAKTU"
        sec.isMockDetected -> "⚠️ FAKE GPS"
        sec.isTimeManipulated -> "⚠️ WAKTU PALSU"
        else -> if (isFilled) "🛡️ ASLI TERVERIFIKASI" else "🛡️ ASLI"
    }
    val borderColor = if (isTampered) Color(0xFFEF4444) else Color(0xFF10B981)
    val textColor = if (isTampered) Color(0xFFFCA5A5) else Color(0xFF34D399)
    val bgColor = if (isFilled) {
        if (isTampered) Color(0xCC7F1D1D) else Color(0xCC064E3B)
    } else {
        if (isTampered) Color(0x997F1D1D) else Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getSafeTime(loc: LiveLocationData): String {
    return loc.formattedTime.ifEmpty { SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date()) }
}

private fun getSafeDate(loc: LiveLocationData): String {
    return loc.formattedDate.ifEmpty { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date()) }
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
                color = Color(0xFF0284C7)
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
            Column(modifier = Modifier.width(145.dp)) {
                Text(
                    text = tag.uppercase(Locale.ROOT),
                    color = Color(0xFF38BDF8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
                Text(
                    text = getSafeTime(loc),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 28.sp,
                    maxLines = 1
                )
                Text(
                    text = getSafeDate(loc),
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                SecurityBadge(sec = loc.securityResult)
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
                Text(getSafeTime(loc), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 30.sp, maxLines = 1)
                Text(getSafeDate(loc), color = Color(0xFFE2E8F0), fontSize = 13.5.sp)
                Spacer(modifier = Modifier.height(4.dp))
                SecurityBadge(sec = loc.securityResult, isFilled = true)
            }

            Spacer(modifier = Modifier.width(12.dp))

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
            color = Color(0xFF38BDF8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------
// DESAIN 3: Kapsul Minimalis (Peta Lingkaran Besar & Teks Besar)
// -------------------------------------------------------------
@Composable
private fun Design3Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    val isTampered = !loc.securityResult.isAuthentic || loc.securityResult.isMockDetected || loc.securityResult.isTimeManipulated
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0x940F172A))
            .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(32.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Real OpenStreetMap
        Box(
            modifier = Modifier
                .size(62.dp)
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
                Text("${getSafeTime(loc)} • ${getSafeDate(loc)}", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
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
                .background(if (isTampered) Color(0xCC7F1D1D) else Color(0xCC0F172A))
                .border(1.5.dp, if (isTampered) Color(0xFFEF4444) else Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isTampered) Icons.Default.Warning else Icons.Default.Check,
                contentDescription = if (isTampered) "Peringatan" else "OK",
                tint = if (isTampered) Color(0xFFFCA5A5) else Color(0xFF10B981),
                modifier = Modifier.size(20.dp)
            )
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
        // Floating Top-Right Map Tab
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

        Spacer(modifier = Modifier.height(10.dp))

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
                    Text("${getSafeTime(loc)} • ${getSafeDate(loc)}", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
                SecurityBadge(sec = loc.securityResult)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
                    val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
                    Text("$latStr, $lonStr", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    SecurityBadge(sec = loc.securityResult)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                    color = Color(0xFFF8FAFC),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tag.uppercase(Locale.ROOT), color = Color(0xFF1D4ED8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(getSafeTime(loc), color = Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                SecurityBadge(sec = loc.securityResult)
            }
            Text(getSafeDate(loc), color = Color(0xFF475569), fontSize = 11.5.sp)
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                color = Color(0xFF1E293B),
                fontSize = 11.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
            val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
            Text(
                text = "$latStr, $lonStr",
                color = Color(0xFF1D4ED8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------
// DESAIN 7: Strip Vertikal Kiri (Peta & Jam Raksasa)
// -------------------------------------------------------------
@Composable
private fun Design7Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    val isTampered = !loc.securityResult.isAuthentic || loc.securityResult.isMockDetected || loc.securityResult.isTimeManipulated
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Vertical Strip (Widened & Enlarged)
        Column(
            modifier = Modifier
                .width(86.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x941E293B))
                .border(1.2.dp, Color(0x44FFFFFF), RoundedCornerShape(18.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RealOsmMapCanvas(
                mapBitmap = mapBitmap,
                modifier = Modifier.size(62.dp),
                isIsometric = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            val parts = getSafeTime(loc).split(":")
            Text(if (parts.isNotEmpty()) parts[0] else "12", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(if (parts.size > 1) parts[1] else "00", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(getSafeDate(loc).replace(" 2026", ""), color = Color(0xFFCBD5E1), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Icon(
                imageVector = if (isTampered) Icons.Default.Warning else Icons.Default.Shield,
                contentDescription = if (isTampered) "Peringatan" else "OK",
                tint = if (isTampered) Color(0xFFEF4444) else Color(0xFF10B981),
                modifier = Modifier.size(18.dp)
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
                val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
                Text("$latStr, $lonStr", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                SecurityBadge(sec = loc.securityResult)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                color = Color(0xFFF8FAFC),
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("SIG: #${loc.securityResult.signatureToken} • LIVE ENCRYPTED", color = Color(0xFFCBD5E1), fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// -------------------------------------------------------------
// DESAIN 8: Segel Kriptografis (Stempel & Teks Besar)
// -------------------------------------------------------------
@Composable
private fun Design8Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    val isTampered = !loc.securityResult.isAuthentic || loc.securityResult.isMockDetected || loc.securityResult.isTimeManipulated
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ENLARGED Circular Crypto Seal
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(if (isTampered) Color(0xEE450A0A) else Color(0xCC0F172A))
                .border(2.2.dp, if (isTampered) Color(0xFFEF4444) else Color(0xFF10B981), CircleShape)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(getSafeTime(loc), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                SecurityBadge(sec = loc.securityResult)
            }
            Text(getSafeDate(loc), color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
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
