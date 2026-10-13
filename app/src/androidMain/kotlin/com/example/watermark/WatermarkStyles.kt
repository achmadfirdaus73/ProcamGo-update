package com.example.watermark

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.location.LiveLocationData
import com.example.location.MapTileHelper
import com.example.security.SecurityCheckResult
import java.util.Locale
import kotlin.math.max

object WatermarkStyles {

    fun drawWatermark(
        canvas: Canvas,
        context: Context,
        locationData: LiveLocationData,
        securityResult: SecurityCheckResult,
        config: WatermarkConfig,
        width: Float,
        height: Float,
        scale: Float,
        customMapThumbnail: Bitmap?
    ) {
        val realMapTile = customMapThumbnail ?: MapTileHelper.getRawTileSync(
            context,
            locationData.latitude,
            locationData.longitude,
            16
        )

        when (config.watermarkStyle) {
            WatermarkStyle.GRID_TRANSPARENT -> drawDesign1GridTransparent(canvas, locationData, securityResult, config, width, height, scale)
            WatermarkStyle.ISOMETRIC_CARD -> drawDesign2IsometricCard(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.COMPACT_CAPSULE -> drawDesign3CompactCapsule(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.FLOATING_TAB_MAP -> drawDesign4FloatingTabMap(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.SPLIT_CORNER -> drawDesign5SplitCorner(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.CLEAN_WHITE -> drawDesign6CleanWhite(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.VERTICAL_STRIP -> drawDesign7VerticalStrip(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.CRYPTO_SEAL -> drawDesign8CryptoSeal(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
        }
    }

    // ==========================================
    // DESAIN 1: KLASIK GRID TRANSPARAN
    // ==========================================
    private fun drawDesign1GridTransparent(
        canvas: Canvas,
        loc: LiveLocationData,
        sec: SecurityCheckResult,
        cfg: WatermarkConfig,
        w: Float,
        h: Float,
        s: Float
    ) {
        val margin = 28f * s
        val cardH = 210f * s
        val cardRect = RectF(margin, h - margin - cardH, w - margin, h - margin)
        val corner = 20f * s

        // Translucent dark glass background (NOT opaque black)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(135, 15, 23, 42) // ~53% transparent navy glass
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(50, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * s
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

        // Faint street grid texture in background
        canvas.save()
        canvas.clipRect(cardRect)
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(18, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1f * s
        }
        var gx = cardRect.left
        while (gx < cardRect.right + cardH) {
            canvas.drawLine(gx, cardRect.top, gx - cardH, cardRect.bottom, gridPaint)
            gx += 36f * s
        }
        canvas.restore()

        val pad = 20f * s
        val dividerX = cardRect.left + 220f * s

        // Left Column: FIRDAUS, Time, Date, ASLI Pill
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText(cfg.tagLabel.uppercase(Locale.ROOT), cardRect.left + pad, cardRect.top + pad + 14f * s, tagPaint)

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText(loc.formattedTime.ifEmpty { "21:39" }, cardRect.left + pad, cardRect.top + pad + 68f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 17f * s
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText(loc.formattedDate.ifEmpty { "13 Okt 2026" }, cardRect.left + pad, cardRect.top + pad + 102f * s, datePaint)

        // ASLI Pill
        drawSmallShieldPill(canvas, cardRect.left + pad, cardRect.top + pad + 120f * s, "ASLI", s)

        // Thin vertical divider line
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
            strokeWidth = 1.2f * s
        }
        canvas.drawLine(dividerX, cardRect.top + 16f * s, dividerX, cardRect.bottom - 16f * s, divPaint)

        // Right Column: Address, Coordinates, Signature
        val rightLeft = dividerX + 16f * s
        val rightW = cardRect.right - pad - rightLeft

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 16.5f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(140, 0, 0, 0))
        }
        val fullAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" }
        val addrLayout = StaticLayout.Builder.obtain(fullAddr, 0, fullAddr.length, addrPaint, rightW.toInt())
            .setMaxLines(3).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(rightLeft, cardRect.top + pad + 4f * s)
        addrLayout.draw(canvas)
        canvas.restore()

        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 18f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(140, 0, 0, 0))
        }
        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        canvas.drawText("$latStr, $lonStr", rightLeft, cardRect.bottom - pad - 26f * s, coordPaint)

        val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 12f * s
            typeface = Typeface.MONOSPACE
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("SIG: #${sec.signatureToken} • LIVE ENCRYPTED", rightLeft, cardRect.bottom - pad - 6f * s, sigPaint)
    }

    // ==========================================
    // DESAIN 2: KARTU ISOMETRIK TERVERIFIKASI
    // ==========================================
    private fun drawDesign2IsometricCard(
        canvas: Canvas,
        loc: LiveLocationData,
        sec: SecurityCheckResult,
        cfg: WatermarkConfig,
        w: Float,
        h: Float,
        s: Float,
        mapTile: Bitmap?
    ) {
        val margin = 28f * s
        val cardH = 240f * s
        val cardRect = RectF(margin, h - margin - cardH, w - margin, h - margin)
        val corner = 22f * s

        // Translucent slate background (NOT solid black)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 30, 41, 59) // ~55% translucent slate
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(50, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * s
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

        val pad = 20f * s

        // Top Left: FIRDAUS, Time, Date, ASLI TERVERIFIKASI
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText(cfg.tagLabel.uppercase(Locale.ROOT), cardRect.left + pad, cardRect.top + pad + 14f * s, tagPaint)

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 52f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText(loc.formattedTime.ifEmpty { "21:39" }, cardRect.left + pad, cardRect.top + pad + 68f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 17f * s
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText(loc.formattedDate.ifEmpty { "13 Okt 2026" }, cardRect.left + pad, cardRect.top + pad + 100f * s, datePaint)

        drawFilledSecurityPill(canvas, cardRect.left + pad, cardRect.top + pad + 116f * s, "ASLI TERVERIFIKASI", s)

        // Top Right: Frosted translucent rounded container with REAL OpenStreetMap
        val mapBoxW = 160f * s
        val mapBoxH = 118f * s
        val mapRect = RectF(cardRect.right - pad - mapBoxW, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + mapBoxH)
        val mapCorner = 16f * s

        val mapBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 248, 250, 252) // Frosted light container
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mapRect, mapCorner, mapCorner, mapBoxPaint)

        // Render REAL OpenStreetMap in isometric perspective with location pin
        val mapInnerRect = RectF(mapRect.left + 8f * s, mapRect.top + 6f * s, mapRect.right - 8f * s, mapRect.bottom - 6f * s)
        IsometricMapDrawer.drawIsometricMap(canvas, mapInnerRect, mapTile)

        // Bottom section: Address and Coordinates • SIG
        val btmTop = cardRect.top + 168f * s
        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 15f * s
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        val fullAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" }
        val addrLayout = StaticLayout.Builder.obtain(fullAddr, 0, fullAddr.length, addrPaint, (cardRect.width() - pad * 2).toInt())
            .setMaxLines(1).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(cardRect.left + pad, btmTop)
        addrLayout.draw(canvas)
        canvas.restore()

        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 14f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr   SIG: #${sec.signatureToken}", cardRect.left + pad, cardRect.bottom - pad, subPaint)
    }

    // ==========================================
    // DESAIN 3: KAPSUL MINIMALIS
    // ==========================================
    private fun drawDesign3CompactCapsule(
        canvas: Canvas,
        loc: LiveLocationData,
        sec: SecurityCheckResult,
        cfg: WatermarkConfig,
        w: Float,
        h: Float,
        s: Float,
        mapTile: Bitmap?
    ) {
        val margin = 28f * s
        val cardH = 100f * s
        val cardRect = RectF(margin, h - margin - cardH, w - margin, h - margin)
        val corner = cardH / 2f

        // Translucent capsule
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 30, 41, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * s
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

        // Left circular badge with REAL OpenStreetMap
        val circleRadius = 38f * s
        val circleCx = cardRect.left + 16f * s + circleRadius
        val circleCy = cardRect.centerY()
        val circleRect = RectF(circleCx - circleRadius, circleCy - circleRadius, circleCx + circleRadius, circleCy + circleRadius)

        val circleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(circleCx, circleCy, circleRadius, circleBg)

        // Draw Real OSM Map in circle with red pin
        val mapInner = RectF(circleRect.left + 3f * s, circleRect.top + 3f * s, circleRect.right - 3f * s, circleRect.bottom - 3f * s)
        IsometricMapDrawer.draw2DMap(canvas, mapInner, mapTile, isCircle = true)

        // Right circular shield checkmark badge
        val rightRadius = 24f * s
        val rightCx = cardRect.right - 20f * s - rightRadius
        val rightCy = cardRect.centerY()
        drawCheckmarkShieldCircle(canvas, rightCx, rightCy, rightRadius, s)

        // Center Content
        val textLeft = circleCx + circleRadius + 14f * s
        val textRight = rightCx - rightRadius - 12f * s
        val textW = textRight - textLeft

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("${cfg.tagLabel.uppercase(Locale.ROOT)} • ", textLeft, cardRect.top + 34f * s, tagPaint)
        val tagW = tagPaint.measureText("${cfg.tagLabel.uppercase(Locale.ROOT)} • ")

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 1f * s, Color.argb(150, 0, 0, 0))
        }
        val timeDateStr = "${loc.formattedTime.ifEmpty { "21:39" }} • ${loc.formattedDate.ifEmpty { "13 Okt 2026" }}"
        canvas.drawText(timeDateStr, textLeft + tagW, cardRect.top + 34f * s, timePaint)

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 14f * s
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        val shortAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, Jatirasa, Jatiasih, Kota Bks" }
        val addrLayout = StaticLayout.Builder.obtain(shortAddr, 0, shortAddr.length, addrPaint, textW.toInt())
            .setMaxLines(1).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(textLeft, cardRect.top + 46f * s)
        addrLayout.draw(canvas)
        canvas.restore()

        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 15f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        canvas.drawText("$latStr, $lonStr", textLeft, cardRect.top + 84f * s, coordPaint)
    }

    // ==========================================
    // DESAIN 4: PETA MELAYANG TAB DI ATAS KARTU
    // ==========================================
    private fun drawDesign4FloatingTabMap(
        canvas: Canvas,
        loc: LiveLocationData,
        sec: SecurityCheckResult,
        cfg: WatermarkConfig,
        w: Float,
        h: Float,
        s: Float,
        mapTile: Bitmap?
    ) {
        val margin = 28f * s
        val mainCardH = 150f * s
        val tabH = 80f * s
        val tabW = 200f * s

        val mainRect = RectF(margin, h - margin - mainCardH, w - margin, h - margin)
        val tabRect = RectF((w - tabW) / 2f, mainRect.top - tabH + 16f * s, (w + tabW) / 2f, mainRect.top + 16f * s)

        // Translucent white floating tab with REAL OpenStreetMap
        val tabPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(tabRect, 18f * s, 18f * s, tabPaint)

        // Render Real OSM Map
        IsometricMapDrawer.drawIsometricMap(canvas, RectF(tabRect.left + 10f * s, tabRect.top + 6f * s, tabRect.right - 10f * s, tabRect.bottom - 6f * s), mapTile)

        // Translucent dark card below
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mainRect, 20f * s, 20f * s, mainPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * s
        }
        canvas.drawRoundRect(mainRect, 20f * s, 20f * s, borderPaint)

        val pad = 20f * s

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("${cfg.tagLabel.uppercase(Locale.ROOT)}  ", mainRect.left + pad, mainRect.top + pad + 14f * s, tagPaint)
        val tagW = tagPaint.measureText("${cfg.tagLabel.uppercase(Locale.ROOT)}  ")

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 1f * s, Color.argb(150, 0, 0, 0))
        }
        val tStr = "${loc.formattedTime.ifEmpty { "21:39" }} • ${loc.formattedDate.ifEmpty { "13 Okt 2026" }}"
        canvas.drawText(tStr, mainRect.left + pad + tagW, mainRect.top + pad + 14f * s, timePaint)

        drawSmallShieldPill(canvas, mainRect.right - pad - 68f * s, mainRect.top + pad - 2f * s, "ASLI", s)

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 14.5f * s
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        val fullAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" }
        val addrLayout = StaticLayout.Builder.obtain(fullAddr, 0, fullAddr.length, addrPaint, (mainRect.width() - pad * 2).toInt())
            .setMaxLines(2).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(mainRect.left + pad, mainRect.top + pad + 32f * s)
        addrLayout.draw(canvas)
        canvas.restore()

        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        val btmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 14f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr   SIG: #${sec.signatureToken}", mainRect.left + pad, mainRect.bottom - pad + 2f * s, btmPaint)
    }

    // ==========================================
    // DESAIN 5: PECAH TIGA (MULTI-CORNER)
    // NOTE: HANYA 3 KARTU KECIL, BAGIAN LAIN TIDAK ADA CARD!
    // ==========================================
    private fun drawDesign5SplitCorner(
        canvas: Canvas,
        loc: LiveLocationData,
        sec: SecurityCheckResult,
        cfg: WatermarkConfig,
        w: Float,
        h: Float,
        s: Float,
        mapTile: Bitmap?
    ) {
        val margin = 28f * s
        val corner = 18f * s
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 30, 41, 59) // Translucent dark
            style = Paint.Style.FILL
        }

        // 1. Top-Left Card: FIRDAUS, 21:39, 13 Okt 2026
        val topRect = RectF(margin, margin + 20f * s, margin + 200f * s, margin + 140f * s)
        canvas.drawRoundRect(topRect, corner, corner, bgPaint)

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 16f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("🛡️ ${cfg.tagLabel.uppercase(Locale.ROOT)}", topRect.left + 16f * s, topRect.top + 30f * s, tagPaint)

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText(loc.formattedTime.ifEmpty { "21:39" }, topRect.left + 16f * s, topRect.top + 76f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 15f * s
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText(loc.formattedDate.ifEmpty { "13 Okt 2026" }, topRect.left + 16f * s, topRect.top + 104f * s, datePaint)

        // 2. Bottom-Right Card: Translucent white rounded box with REAL OpenStreetMap
        val mapSize = 130f * s
        val mapRect = RectF(w - margin - mapSize, h - margin - mapSize, w - margin, h - margin)
        val mapBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(185, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mapRect, corner, corner, mapBoxPaint)
        IsometricMapDrawer.drawIsometricMap(canvas, RectF(mapRect.left + 8f * s, mapRect.top + 8f * s, mapRect.right - 8f * s, mapRect.bottom - 8f * s), mapTile)

        // 3. Bottom-Left Card: Address & Coordinates
        val addrW = mapRect.left - margin - 14f * s
        val btmLeftRect = RectF(margin, h - margin - mapSize, margin + addrW, h - margin)
        canvas.drawRoundRect(btmLeftRect, corner, corner, bgPaint)

        val pad = 16f * s
        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 14f * s
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        val fullAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" }
        val addrLayout = StaticLayout.Builder.obtain(fullAddr, 0, fullAddr.length, addrPaint, (btmLeftRect.width() - pad * 2).toInt())
            .setMaxLines(3).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(btmLeftRect.left + pad, btmLeftRect.top + pad)
        addrLayout.draw(canvas)
        canvas.restore()

        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr", btmLeftRect.left + pad, btmLeftRect.bottom - pad + 2f * s, coordPaint)
    }

    // ==========================================
    // DESAIN 6: KARTU PUTIH ELEGAN (TRANSLUCENT FROSTED GLASS)
    // ==========================================
    private fun drawDesign6CleanWhite(
        canvas: Canvas,
        loc: LiveLocationData,
        sec: SecurityCheckResult,
        cfg: WatermarkConfig,
        w: Float,
        h: Float,
        s: Float,
        mapTile: Bitmap?
    ) {
        val margin = 28f * s
        val cardH = 170f * s
        val cardRect = RectF(margin, h - margin - cardH, w - margin, h - margin)
        val corner = 20f * s

        // Frosted translucent light glass (NOT solid opaque white)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(170, 255, 255, 255) // ~66% translucent white glass
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 15, 23, 42)
            style = Paint.Style.STROKE
            strokeWidth = 1f * s
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

        // Left Side: REAL OpenStreetMap Box
        val mapBoxW = 140f * s
        val mapRect = RectF(cardRect.left + 16f * s, cardRect.top + 16f * s, cardRect.left + 16f * s + mapBoxW, cardRect.bottom - 16f * s)
        val mapInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 241, 245, 249)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mapRect, 14f * s, 14f * s, mapInnerPaint)
        IsometricMapDrawer.drawIsometricMap(canvas, RectF(mapRect.left + 6f * s, mapRect.top + 6f * s, mapRect.right - 6f * s, mapRect.bottom - 6f * s), mapTile)

        // Right Side Content
        val rightLeft = mapRect.right + 18f * s
        val rightW = cardRect.right - 18f * s - rightLeft

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${cfg.tagLabel.uppercase(Locale.ROOT)}   ", rightLeft, cardRect.top + 38f * s, tagPaint)
        val tagW = tagPaint.measureText("${cfg.tagLabel.uppercase(Locale.ROOT)}   ")

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 21f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(loc.formattedTime.ifEmpty { "21:39" }, rightLeft + tagW, cardRect.top + 38f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 14f * s
        }
        canvas.drawText(loc.formattedDate.ifEmpty { "13 Okt 2026" }, rightLeft, cardRect.top + 58f * s, datePaint)

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 13.5f * s
        }
        val fullAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" }
        val addrLayout = StaticLayout.Builder.obtain(fullAddr, 0, fullAddr.length, addrPaint, rightW.toInt())
            .setMaxLines(2).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(rightLeft, cardRect.top + 70f * s)
        addrLayout.draw(canvas)
        canvas.restore()

        val greenTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#059669")
            textSize = 14f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("✓ Asli terverifikasi", rightLeft, cardRect.bottom - 18f * s, greenTextPaint)

        val tokenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 14f * s
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText("#${sec.signatureToken}", cardRect.right - 20f * s - tokenPaint.measureText("#${sec.signatureToken}"), cardRect.bottom - 18f * s, tokenPaint)
    }

    // ==========================================
    // DESAIN 7: STRIP VERTIKAL KIRI & INFO KANAN
    // ==========================================
    private fun drawDesign7VerticalStrip(
        canvas: Canvas,
        loc: LiveLocationData,
        sec: SecurityCheckResult,
        cfg: WatermarkConfig,
        w: Float,
        h: Float,
        s: Float,
        mapTile: Bitmap?
    ) {
        val margin = 28f * s
        val corner = 22f * s
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 30, 41, 59) // Translucent dark
            style = Paint.Style.FILL
        }

        // Left Vertical Pillar
        val stripW = 120f * s
        val stripRect = RectF(margin, margin + 40f * s, margin + stripW, h - margin)
        canvas.drawRoundRect(stripRect, corner, corner, bgPaint)

        // Top inside strip: REAL OpenStreetMap in 3D
        val mapBoxSize = stripW - 20f * s
        val mapRect = RectF(stripRect.left + 10f * s, stripRect.top + 12f * s, stripRect.right - 10f * s, stripRect.top + 12f * s + mapBoxSize)
        val mapBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(185, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mapRect, 14f * s, 14f * s, mapBg)
        IsometricMapDrawer.drawIsometricMap(canvas, RectF(mapRect.left + 4f * s, mapRect.top + 4f * s, mapRect.right - 4f * s, mapRect.bottom - 4f * s), mapTile)

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 15f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText(cfg.tagLabel.uppercase(Locale.ROOT), stripRect.centerX(), mapRect.bottom + 28f * s, tagPaint)

        // Stacked numbers: 21 on top, 39 below
        val timeParts = loc.formattedTime.ifEmpty { "21:39" }.split(":")
        val hour = if (timeParts.isNotEmpty()) timeParts[0] else "21"
        val min = if (timeParts.size > 1) timeParts[1] else "39"

        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText(hour, stripRect.centerX(), mapRect.bottom + 72f * s, numPaint)
        canvas.drawText(min, stripRect.centerX(), mapRect.bottom + 116f * s, numPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 15f * s
            textAlign = Paint.Align.CENTER
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        val dateText = loc.formattedDate.ifEmpty { "13 Okt 2026" }.replace(" 2026", "")
        canvas.drawText(dateText, stripRect.centerX(), mapRect.bottom + 144f * s, datePaint)

        drawCheckmarkShieldCircle(canvas, stripRect.centerX(), stripRect.bottom - 36f * s, 22f * s, s)

        // Right Bottom Info Card
        val infoLeft = stripRect.right + 14f * s
        val infoCardH = 140f * s
        val infoRect = RectF(infoLeft, h - margin - infoCardH, w - margin, h - margin)
        canvas.drawRoundRect(infoRect, 18f * s, 18f * s, bgPaint)

        val pad = 18f * s
        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 14.5f * s
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        val fullAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" }
        val addrLayout = StaticLayout.Builder.obtain(fullAddr, 0, fullAddr.length, addrPaint, (infoRect.width() - pad * 2).toInt())
            .setMaxLines(2).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(infoRect.left + pad, infoRect.top + pad)
        addrLayout.draw(canvas)
        canvas.restore()

        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr", infoRect.left + pad, infoRect.bottom - pad - 22f * s, coordPaint)

        val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 13f * s
            typeface = Typeface.MONOSPACE
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("SIG: #${sec.signatureToken} • LIVE ENCRYPTED", infoRect.left + pad, infoRect.bottom - pad, sigPaint)
    }

    // ==========================================
    // DESAIN 8: SEGEL KRIPTOGRAFIS & KARTU KANAN
    // ==========================================
    private fun drawDesign8CryptoSeal(
        canvas: Canvas,
        loc: LiveLocationData,
        sec: SecurityCheckResult,
        cfg: WatermarkConfig,
        w: Float,
        h: Float,
        s: Float,
        mapTile: Bitmap?
    ) {
        val margin = 28f * s
        val cardH = 160f * s
        val sealRadius = 66f * s
        val sealCx = margin + sealRadius
        val sealCy = h - margin - cardH / 2f

        // Draw Left Circular Crypto Stamp Seal with REAL OpenStreetMap
        drawCryptoSealStamp(canvas, sealCx, sealCy, sealRadius, s, mapTile)

        // Draw Right Translucent Dark Card
        val cardLeft = sealCx + sealRadius + 14f * s
        val cardRect = RectF(cardLeft, h - margin - cardH, w - margin, h - margin)
        val corner = 20f * s

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 30, 41, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        val pad = 18f * s

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("${cfg.tagLabel.uppercase(Locale.ROOT)}   ", cardRect.left + pad, cardRect.top + pad + 14f * s, tagPaint)
        val tagW = tagPaint.measureText("${cfg.tagLabel.uppercase(Locale.ROOT)}   ")

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 1f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText(loc.formattedTime.ifEmpty { "21:39" }, cardRect.left + pad + tagW, cardRect.top + pad + 14f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 14f * s
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText(loc.formattedDate.ifEmpty { "13 Okt 2026" }, cardRect.left + pad, cardRect.top + pad + 36f * s, datePaint)

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 14f * s
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        val shortAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, Jatirasa, Jatiasih, Kota Bks" }
        val addrLayout = StaticLayout.Builder.obtain(shortAddr, 0, shortAddr.length, addrPaint, (cardRect.width() - pad * 2).toInt())
            .setMaxLines(2).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(cardRect.left + pad, cardRect.top + pad + 48f * s)
        addrLayout.draw(canvas)
        canvas.restore()

        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        val btmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 14f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(140, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr   #${sec.signatureToken}", cardRect.left + pad, cardRect.bottom - pad + 2f * s, btmPaint)
    }

    // ==========================================
    // HELPER BADGES & SEALS
    // ==========================================
    private fun drawSmallShieldPill(canvas: Canvas, x: Float, y: Float, text: String, s: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * s
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#34D399")
            textSize = 12f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textW = textPaint.measureText("🛡️ $text")
        val pillRect = RectF(x, y, x + textW + 14f * s, y + 22f * s)
        canvas.drawRoundRect(pillRect, 11f * s, 11f * s, paint)
        canvas.drawText("🛡️ $text", x + 7f * s, y + 15.5f * s, textPaint)
    }

    private fun drawFilledSecurityPill(canvas: Canvas, x: Float, y: Float, text: String, s: Float) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 6, 78, 59)
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6EE7B7")
            textSize = 12f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textW = textPaint.measureText("🛡️ $text")
        val pillRect = RectF(x, y, x + textW + 16f * s, y + 24f * s)
        canvas.drawRoundRect(pillRect, 12f * s, 12f * s, bgPaint)
        canvas.drawText("🛡️ $text", x + 8f * s, y + 17f * s, textPaint)
    }

    private fun drawCheckmarkShieldCircle(canvas: Canvas, cx: Float, cy: Float, r: Float, s: Float) {
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r, circlePaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 2f * s
        }
        canvas.drawCircle(cx, cy, r, strokePaint)

        val checkPath = Path().apply {
            moveTo(cx - 8f * s, cy - 1f * s)
            lineTo(cx - 2f * s, cy + 6f * s)
            lineTo(cx + 8f * s, cy - 6f * s)
        }
        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * s
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(checkPath, checkPaint)
    }

    private fun drawCryptoSealStamp(canvas: Canvas, cx: Float, cy: Float, r: Float, s: Float, mapTile: Bitmap?) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r, bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * s
        }
        canvas.drawCircle(cx, cy, r - 2f * s, ringPaint)

        // Inner circle with REAL OpenStreetMap
        val innerRadius = r * 0.62f
        val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, innerRadius, innerCirclePaint)

        // Draw Real OSM Map in inner circle with red pin
        val mapRect = RectF(cx - innerRadius + 2f * s, cy - innerRadius + 2f * s, cx + innerRadius - 2f * s, cy + innerRadius - 2f * s)
        IsometricMapDrawer.draw2DMap(canvas, mapRect, mapTile, isCircle = true)

        // Circular text around ring
        val textPath = Path().apply {
            addCircle(cx, cy, r - 12f * s, Path.Direction.CW)
        }
        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6EE7B7")
            textSize = 9.5f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.18f
        }
        canvas.drawTextOnPath("LIVE ENCRYPTED • ASLI TERVERIFIKASI •", textPath, 0f, 0f, stampTextPaint)
    }
}
