package com.example.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.camera.PhotoQualityEngine
import com.example.camera.PhotoQualityStyle
import com.example.location.LiveLocationData
import com.example.location.MapTileHelper
import com.example.security.SecurityCheckResult
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class WatermarkStyle(val id: Int, val title: String, val desc: String) {
    GRID_TRANSPARENT(1, "Desain 1: Klasik Grid Presisi", "Kartu navy transparan bergaris grid peta & pembatas"),
    ISOMETRIC_CARD(2, "Desain 2: Kartu Isometrik Terverifikasi", "Kartu gelap lebar dengan miniatur peta 3D di kanan"),
    COMPACT_CAPSULE(3, "Desain 3: Kapsul Minimalis", "Format kapsul horizontal dengan lingkaran peta & perisai"),
    FLOATING_TAB_MAP(4, "Desain 4: Peta Melayang Tab", "Tab peta 3D menonjol di atas kartu data"),
    SPLIT_CORNER(5, "Desain 5: Pecah Sudut (Multi-Card)", "Jam di kiri-atas, info di kiri-bawah, peta di kanan-bawah"),
    CLEAN_WHITE(6, "Desain 6: Kartu Putih Elegan", "Kartu putih bersih dengan teks kontras tinggi & peta 3D"),
    VERTICAL_STRIP(7, "Desain 7: Strip Vertikal Kiri", "Pilar waktu vertikal di sisi kiri & info alamat di kanan"),
    CRYPTO_SEAL(8, "Desain 8: Segel Kriptografis", "Stempel melingkar kripto di kiri & kartu data di kanan");

    companion object {
        fun fromString(value: String): WatermarkStyle {
            return try {
                valueOf(value)
            } catch (_: Exception) {
                when (value) {
                    "MODERN_DARK_BOTTOM_LEFT" -> GRID_TRANSPARENT
                    "MODERN_DARK_BOTTOM_RIGHT" -> ISOMETRIC_CARD
                    "MODERN_LIGHT_BOTTOM_RIGHT" -> CLEAN_WHITE
                    "MODERN_LIGHT_BOTTOM_LEFT" -> FLOATING_TAB_MAP
                    "MINIMAL_DARK_TOP_LEFT" -> SPLIT_CORNER
                    "MODERN_LIGHT_BOTTOM_CENTER" -> COMPACT_CAPSULE
                    "VERTICAL_DARK_LEFT" -> VERTICAL_STRIP
                    "ROUNDED_DARK_BOTTOM_LEFT" -> CRYPTO_SEAL
                    else -> GRID_TRANSPARENT
                }
            }
        }
    }
}

data class WatermarkConfig(
    val tagLabel: String = "FIRDAUS",
    val qualityPercentage: Int = 95,
    val targetWidth: Int = 1920,
    val targetHeight: Int = 2560,
    val showDynamicMap: Boolean = true,
    val showSecurityToken: Boolean = true,
    val qualityStyle: PhotoQualityStyle = PhotoQualityStyle.SMART_HDR_ULTRA,
    val watermarkStyle: WatermarkStyle = WatermarkStyle.GRID_TRANSPARENT
)

data class WatermarkResult(
    val bitmap: Bitmap,
    val jpegBytes: ByteArray,
    val fileSizeBytes: Long,
    val originalSizeBytes: Long,
    val compressionRatioPercent: Int,
    val integrityHash: String
)

object WatermarkRenderer {

    fun applyWatermarkAndCompress(
        context: Context,
        sourceBitmap: Bitmap,
        locationData: LiveLocationData,
        securityResult: SecurityCheckResult,
        config: WatermarkConfig,
        customMapThumbnail: Bitmap? = null
    ): WatermarkResult {
        val scaledSource = scaleToTargetResolution(sourceBitmap, config.targetWidth, config.targetHeight)
        val enhanced = PhotoQualityEngine.processBitmap(scaledSource, config.qualityStyle)
        val originalRawSize = (enhanced.width * enhanced.height * 4).toLong()

        val outputBitmap = enhanced.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)

        val width = outputBitmap.width.toFloat()
        val height = outputBitmap.height.toFloat()
        val scale = max(width / 1080f, 0.5f)

        // Draw exact chosen watermark design
        WatermarkStyles.drawWatermark(
            canvas = canvas,
            context = context,
            locationData = locationData,
            securityResult = securityResult,
            config = config,
            width = width,
            height = height,
            scale = scale,
            customMapThumbnail = customMapThumbnail
        )

        val stream = ByteArrayOutputStream()
        outputBitmap.compress(Bitmap.CompressFormat.JPEG, config.qualityPercentage.coerceIn(40, 100), stream)
        val compressedBytes = stream.toByteArray()

        return WatermarkResult(
            bitmap = outputBitmap,
            jpegBytes = compressedBytes,
            fileSizeBytes = compressedBytes.size.toLong(),
            originalSizeBytes = originalRawSize,
            compressionRatioPercent = 0,
            integrityHash = securityResult.integrityHash
        )
    }

    private fun drawModernGlassStyle(
        canvas: Canvas, context: Context, locationData: LiveLocationData,
        securityResult: SecurityCheckResult, config: WatermarkConfig,
        cardRect: RectF, cornerRadius: Float, scale: Float, customMapThumbnail: Bitmap?
    ) {
        val isTampered = securityResult.isMockDetected || securityResult.isTimeManipulated
        canvas.save()
        val path = Path().apply { addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW) }
        canvas.clipPath(path)

        val mapTile = customMapThumbnail ?: MapTileHelper.getRawTileSync(context, locationData.latitude, locationData.longitude, 16)
        canvas.drawBitmap(mapTile, null, cardRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = 135 })
        canvas.drawRect(cardRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(170, 15, 23, 42); style = Paint.Style.FILL })
        canvas.restore()

        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.argb(180, 239, 68, 68) else Color.argb(70, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2.0f * scale
        })

        drawStandardContent(canvas, locationData, securityResult, config, cardRect, scale)
    }

    private fun drawExecutiveBadgeStyle(
        canvas: Canvas, context: Context, locationData: LiveLocationData,
        securityResult: SecurityCheckResult, config: WatermarkConfig,
        cardRect: RectF, cornerRadius: Float, scale: Float, customMapThumbnail: Bitmap?
    ) {
        canvas.save()
        val path = Path().apply { addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW) }
        canvas.clipPath(path)

        canvas.drawRect(cardRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0F172A"); style = Paint.Style.FILL })
        // Corporate blue banner header
        val headerRect = RectF(cardRect.left, cardRect.top, cardRect.right, cardRect.top + 45f * scale)
        canvas.drawRect(headerRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E3A8A"); style = Paint.Style.FILL })
        canvas.restore()

        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B82F6")
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * scale
        })

        val pad = 20f * scale
        // Draw Header Tag
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        canvas.drawText("EXECUTIVE AUDIT • ${config.tagLabel.uppercase(Locale.ROOT)}", cardRect.left + pad, cardRect.top + 28f * scale, tagPaint)

        drawStandardContent(canvas, locationData, securityResult, config, RectF(cardRect.left, cardRect.top + 40f * scale, cardRect.right, cardRect.bottom), scale)
    }

    private fun drawCyberpunkHudStyle(
        canvas: Canvas, context: Context, locationData: LiveLocationData,
        securityResult: SecurityCheckResult, config: WatermarkConfig,
        cardRect: RectF, cornerRadius: Float, scale: Float, customMapThumbnail: Bitmap?
    ) {
        canvas.save()
        val path = Path().apply { addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW) }
        canvas.clipPath(path)
        canvas.drawRect(cardRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#050B14"); style = Paint.Style.FILL })
        canvas.restore()

        // Neon cyan/emerald borders
        val hudBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * scale
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, hudBorder)

        drawStandardContent(canvas, locationData, securityResult, config, cardRect, scale)
    }

    private fun drawClassicStampStyle(
        canvas: Canvas, context: Context, locationData: LiveLocationData,
        securityResult: SecurityCheckResult, config: WatermarkConfig,
        cardRect: RectF, cornerRadius: Float, scale: Float, customMapThumbnail: Bitmap?
    ) {
        canvas.save()
        val path = Path().apply { addRoundRect(cardRect, cornerRadius, cornerRadius, Path.Direction.CW) }
        canvas.clipPath(path)
        canvas.drawRect(cardRect, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1E293B"); style = Paint.Style.FILL })
        canvas.restore()

        val stampBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }
        val innerRect = RectF(cardRect.left + 6f * scale, cardRect.top + 6f * scale, cardRect.right - 6f * scale, cardRect.bottom - 6f * scale)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, stampBorder)
        canvas.drawRoundRect(innerRect, cornerRadius - 4f, cornerRadius - 4f, stampBorder)

        drawStandardContent(canvas, locationData, securityResult, config, cardRect, scale)
    }

    private fun drawStandardContent(
        canvas: Canvas, locationData: LiveLocationData, securityResult: SecurityCheckResult,
        config: WatermarkConfig, cardRect: RectF, scale: Float
    ) {
        val pad = 24f * scale
        val contentLeft = cardRect.left + pad
        val contentTop = cardRect.top + pad
        val contentBottom = cardRect.bottom - pad
        val contentRight = cardRect.right - pad

        val leftColWidth = 240f * scale
        val shadowRadius = 4f * scale
        val shadowDy = 2f * scale
        val shadowColor = Color.argb(180, 0, 0, 0)

        // 1. Tag Label
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 20f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.15f
            setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        }
        canvas.drawText(
            config.tagLabel.uppercase(Locale.ROOT),
            contentLeft,
            contentTop + 20f * scale,
            tagPaint
        )

        // 2. Big Clock
        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 58f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(shadowRadius * 1.5f, 0f, shadowDy * 1.5f, shadowColor)
        }
        val timeText = locationData.formattedTime.ifEmpty {
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale("id", "ID")).format(java.util.Date())
        }
        canvas.drawText(timeText, contentLeft, contentTop + 84f * scale, clockPaint)

        // 3. Date
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 21f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        }
        val dateText = locationData.formattedDate.ifEmpty {
            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(java.util.Date())
        }
        canvas.drawText(dateText, contentLeft, contentTop + 124f * scale, datePaint)

        // 4. Status Pill
        drawSecurityPill(canvas, securityResult, contentLeft, contentTop + 158f * scale, scale)

        // Divider
        val dividerX = contentLeft + leftColWidth + 12f * scale
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
            strokeWidth = 1.5f * scale
        }
        canvas.drawLine(dividerX, contentTop + 4f * scale, dividerX, contentBottom - 4f * scale, dividerPaint)

        // Right details (Address + Coords)
        val midLeft = dividerX + 18f * scale
        val midRight = contentRight
        val midWidth = max(midRight - midLeft, 100f)

        val addressPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 19f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        }

        val fullAddress = locationData.address.ifEmpty { "Jl. Raya No. 1, Jakarta, Indonesia" }

        val addressLayout = StaticLayout.Builder.obtain(
            fullAddress, 0, fullAddress.length, addressPaint, midWidth.toInt()
        ).setMaxLines(3).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()

        canvas.save()
        canvas.translate(midLeft, contentTop + 6f * scale)
        addressLayout.draw(canvas)
        canvas.restore()

        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 21f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        }

        val latStr = String.format(Locale.US, "%.6f", locationData.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", locationData.longitude).replace('.', ',')
        val coordText = "$latStr, $lonStr"

        canvas.drawText(coordText, midLeft, contentBottom - 26f * scale, coordPaint)

        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 13f * scale
            typeface = Typeface.MONOSPACE
            setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        }
        canvas.drawText("SIG: #${securityResult.signatureToken} • LIVE ENCRYPTED", midLeft, contentBottom - 6f * scale, stampPaint)
    }

    private fun drawSecurityPill(canvas: Canvas, security: SecurityCheckResult, x: Float, y: Float, scale: Float) {
        val isTampered = security.isMockDetected || security.isTimeManipulated
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.argb(210, 127, 29, 29) else Color.argb(200, 6, 78, 59)
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.parseColor("#FCA5A5") else Color.parseColor("#6EE7B7")
            textSize = 12f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val label = when {
            security.isTimeManipulated && security.isMockDetected -> "⚠️ WAKTU & GPS PALSU"
            security.isTimeManipulated -> "⚠️ WAKTU MANUAL (PALSU)"
            security.isMockDetected -> "⚠️ FAKE GPS"
            else -> "🛡️ ASLI TERVERIFIKASI"
        }
        val textWidth = textPaint.measureText(label)
        val pillHeight = 22f * scale
        val pillWidth = textWidth + (16f * scale)
        val pillRect = RectF(x, y, x + pillWidth, y + pillHeight)

        canvas.drawRoundRect(pillRect, pillHeight / 2f, pillHeight / 2f, pillPaint)
        canvas.drawText(label, x + (8f * scale), y + (15.5f * scale), textPaint)
    }

    private fun scaleToTargetResolution(bitmap: Bitmap, targetShort: Int = 1920, targetLong: Int = 2560): Bitmap {
        val isPortrait = bitmap.height >= bitmap.width
        val targetW = if (isPortrait) targetShort else targetLong
        val targetH = if (isPortrait) targetLong else targetShort

        val targetAspect = targetW.toFloat() / targetH.toFloat()
        val currentAspect = bitmap.width.toFloat() / bitmap.height.toFloat()

        // Jika rasio aspek kamera sedikit berbeda (misal 0.738 vs 0.75), lakukan center-crop simetris agar objek tidak gepeng/terdistorsi
        val croppedBitmap = if (kotlin.math.abs(currentAspect - targetAspect) > 0.005f) {
            if (currentAspect > targetAspect) {
                // Gambar lebih lebar daripada 3:4 -> potong sisi kiri dan kanan secara simetris
                val newWidth = (bitmap.height * targetAspect).toInt().coerceIn(1, bitmap.width)
                val xOffset = ((bitmap.width - newWidth) / 2).coerceAtLeast(0)
                Bitmap.createBitmap(bitmap, xOffset, 0, newWidth, bitmap.height)
            } else {
                // Gambar lebih tinggi daripada 3:4 -> potong atas dan bawah secara simetris
                val newHeight = (bitmap.width / targetAspect).toInt().coerceIn(1, bitmap.height)
                val yOffset = ((bitmap.height - newHeight) / 2).coerceAtLeast(0)
                Bitmap.createBitmap(bitmap, 0, yOffset, bitmap.width, newHeight)
            }
        } else {
            bitmap
        }

        // Skala persis ke target 1920 x 2560 pixel
        return Bitmap.createScaledBitmap(croppedBitmap, targetW, targetH, true)
    }
}
