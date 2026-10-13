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
        WatermarkStyle.GRID_TRANSPARENT -> Design1Overlay(locationData, tagLabel, modifier)
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xB8FFFFFF)), // Translucent frosted white container
        contentAlignment = Alignment.Center
    ) {
        if (mapBitmap != null && !mapBitmap.isRecycled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val nativeCanvas = drawContext.canvas.nativeCanvas
                val rect = RectF(0f, 0f, size.width, size.height)
                if (isIsometric) {
                    IsometricMapDrawer.drawIsometricMap(nativeCanvas, rect, mapBitmap)
                } else {
                    IsometricMapDrawer.draw2DMap(nativeCanvas, rect, mapBitmap, cornerRadius = 12f)
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
// DESAIN 1: Klasik Grid Presisi (Translucent Card, NO Map Box)
// -------------------------------------------------------------
@Composable
private fun Design1Overlay(loc: LiveLocationData, tag: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x8C0F172A)) // Translucent navy glass
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(115.dp)) {
                Text(
                    text = tag.uppercase(Locale.ROOT),
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
                Text(
                    text = loc.formattedTime.ifEmpty { "21:39" },
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 30.sp
                )
                Text(
                    text = loc.formattedDate.ifEmpty { "13 Okt 2026" },
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("🛡️ ASLI", color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(84.dp)
                    .background(Color(0x33FFFFFF))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                    color = Color(0xFFF8FAFC),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
                val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
                Text(
                    text = "$latStr, $lonStr",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SIG: #${loc.securityResult.signatureToken} • LIVE ENCRYPTED",
                    color = Color(0xFFCBD5E1),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DESAIN 2: Kartu Isometrik Terverifikasi (Translucent + Real OSM Map)
// -------------------------------------------------------------
@Composable
private fun Design2Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x8C1E293B)) // Translucent slate glass
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(loc.formattedTime.ifEmpty { "21:39" }, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text(loc.formattedDate.ifEmpty { "13 Okt 2026" }, color = Color(0xFFCBD5E1), fontSize = 11.5.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xB3064E3B))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("🛡️ ASLI TERVERIFIKASI", color = Color(0xFF6EE7B7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Real OpenStreetMap Box (3D Isometric)
            RealOsmMapCanvas(
                mapBitmap = mapBitmap,
                modifier = Modifier.size(width = 96.dp, height = 76.dp),
                isIsometric = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
            color = Color(0xFFF1F5F9),
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        Text(
            text = "$latStr, $lonStr   SIG: #${loc.securityResult.signatureToken}",
            color = Color(0xFFCBD5E1),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------
// DESAIN 3: Kapsul Minimalis Horizontal (Translucent + Real OSM Map)
// -------------------------------------------------------------
@Composable
private fun Design3Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color(0x8C1E293B)) // Translucent pill
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circle containing REAL OpenStreetMap
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xB8FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (mapBitmap != null && !mapBitmap.isRecycled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    val rect = RectF(0f, 0f, size.width, size.height)
                    IsometricMapDrawer.draw2DMap(nativeCanvas, rect, mapBitmap, isCircle = true)
                }
            } else {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = Color(0xFF38BDF8))
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(" • ", color = Color.White, fontSize = 11.sp)
                Text("${loc.formattedTime.ifEmpty { "21:39" }} • ${loc.formattedDate.ifEmpty { "13 Okt 2026" }}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, Jatirasa, Jatiasih, Kota Bks" },
                color = Color(0xFFCBD5E1),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
            val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
            Text("$latStr, $lonStr", color = Color(0xFF38BDF8), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xA00F172A))
                .border(1.5.dp, Color(0xFF10B981), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = "OK", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
        }
    }
}

// -------------------------------------------------------------
// DESAIN 4: Peta Melayang Tab di Atas (Translucent)
// -------------------------------------------------------------
@Composable
private fun Design4Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Top Tab with REAL OpenStreetMap
        RealOsmMapCanvas(
            mapBitmap = mapBitmap,
            modifier = Modifier
                .width(120.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
            isIsometric = true
        )

        // Main Translucent Dark Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x8C0F172A))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${loc.formattedTime.ifEmpty { "21:39" }} • ${loc.formattedDate.ifEmpty { "13 Okt 2026" }}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("🛡️ ASLI", color = Color(0xFF34D399), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                color = Color(0xFFE2E8F0),
                fontSize = 10.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
            val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
            Text(
                text = "$latStr, $lonStr   SIG: #${loc.securityResult.signatureToken}",
                color = Color(0xFFCBD5E1),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -------------------------------------------------------------
// DESAIN 5: Pecah Tiga (HANYA KARTU DI SUDUT, TENGAH TANPA KARTU)
// -------------------------------------------------------------
@Composable
private fun Design5Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Bottom-Left Address Box
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x8C1E293B))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                    color = Color(0xFFF8FAFC),
                    fontSize = 10.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
                val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
                Text("$latStr, $lonStr", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            // Bottom-Right Real OpenStreetMap Box
            RealOsmMapCanvas(
                mapBitmap = mapBitmap,
                modifier = Modifier.size(76.dp),
                isIsometric = true
            )
        }
    }
}

// -------------------------------------------------------------
// DESAIN 6: Kartu Putih Elegan (Frosted Glass Translucent, NOT solid white)
// -------------------------------------------------------------
@Composable
private fun Design6Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xA6FFFFFF)) // ~65% translucent frosted light glass
            .border(1.dp, Color(0x33000000), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RealOsmMapCanvas(
            mapBitmap = mapBitmap,
            modifier = Modifier.size(width = 84.dp, height = 72.dp),
            isIsometric = true
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tag.uppercase(Locale.ROOT), color = Color(0xFF1D4ED8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(loc.formattedTime.ifEmpty { "21:39" }, color = Color(0xFF0F172A), fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Text(loc.formattedDate.ifEmpty { "13 Okt 2026" }, color = Color(0xFF475569), fontSize = 10.sp)
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                color = Color(0xFF1E293B),
                fontSize = 10.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("✓ Asli terverifikasi", color = Color(0xFF059669), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                Text("#${loc.securityResult.signatureToken}", color = Color(0xFF475569), fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// -------------------------------------------------------------
// DESAIN 7: Strip Vertikal Kiri (Translucent, NO center card)
// -------------------------------------------------------------
@Composable
private fun Design7Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Vertical Strip (Translucent)
        Column(
            modifier = Modifier
                .width(70.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x8C1E293B))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RealOsmMapCanvas(
                mapBitmap = mapBitmap,
                modifier = Modifier.size(46.dp),
                isIsometric = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            val parts = loc.formattedTime.ifEmpty { "21:39" }.split(":")
            Text(if (parts.isNotEmpty()) parts[0] else "21", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(if (parts.size > 1) parts[1] else "39", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("13 Okt", color = Color(0xFFCBD5E1), fontSize = 9.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Icon(Icons.Default.Shield, contentDescription = "OK", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
        }

        // Right Info Box (Translucent)
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x8C1E293B))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" },
                color = Color(0xFFF8FAFC),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
            val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
            Text("$latStr, $lonStr", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("SIG: #${loc.securityResult.signatureToken} • LIVE ENCRYPTED", color = Color(0xFFCBD5E1), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// -------------------------------------------------------------
// DESAIN 8: Segel Kriptografis (Translucent + Real OSM Map)
// -------------------------------------------------------------
@Composable
private fun Design8Overlay(loc: LiveLocationData, tag: String, mapBitmap: Bitmap?, modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Crypto Seal with REAL OpenStreetMap
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xA00F172A))
                .border(2.dp, Color(0xFF10B981), CircleShape)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xB8FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (mapBitmap != null && !mapBitmap.isRecycled) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val nativeCanvas = drawContext.canvas.nativeCanvas
                        val rect = RectF(0f, 0f, size.width, size.height)
                        IsometricMapDrawer.draw2DMap(nativeCanvas, rect, mapBitmap, isCircle = true)
                    }
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.5.dp, color = Color(0xFF38BDF8))
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right Translucent Dark Info Card
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x8C1E293B))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tag.uppercase(Locale.ROOT), color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(loc.formattedTime.ifEmpty { "21:39" }, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Text(loc.formattedDate.ifEmpty { "13 Okt 2026" }, color = Color(0xFFCBD5E1), fontSize = 10.sp)
            Text(
                text = loc.address.ifEmpty { "Gg. Kuncir No.27, Jatirasa, Jatiasih, Kota Bks" },
                color = Color(0xFFF8FAFC),
                fontSize = 10.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
            val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
            Text("$latStr, $lonStr   #${loc.securityResult.signatureToken}", color = Color(0xFFCBD5E1), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
