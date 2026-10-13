package com.example.watermark

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.location.LiveLocationData
import com.example.location.MapTileHelper
import com.example.security.SecurityCheckResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            WatermarkStyle.GRID_TRANSPARENT -> drawDesign1GridTransparent(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.ISOMETRIC_CARD -> drawDesign2IsometricCard(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.COMPACT_CAPSULE -> drawDesign3CompactCapsule(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.FLOATING_TAB_MAP -> drawDesign4FloatingTabMap(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.SPLIT_CORNER -> drawDesign5SplitCorner(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.CLEAN_WHITE -> drawDesign6CleanWhite(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.VERTICAL_STRIP -> drawDesign7VerticalStrip(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
            WatermarkStyle.CRYPTO_SEAL -> drawDesign8CryptoSeal(canvas, locationData, securityResult, config, width, height, scale, realMapTile)
        }
    }

    // ==============================================================
    // DESAIN 1: KLASIK GRID PRESISI (MAP JADI BACKGROUND KARTU!)
    // ==============================================================
    private fun drawDesign1GridTransparent(
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

        val cardPath = Path().apply {
            addRoundRect(cardRect, corner, corner, Path.Direction.CW)
        }

        // 1. Draw Real OpenStreetMap across entire card background!
        canvas.save()
        canvas.clipPath(cardPath)

        if (mapTile != null && !mapTile.isRecycled) {
            val mapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = 95 // Subtle real street map showing through
            }
            canvas.drawBitmap(mapTile, null, cardRect, mapPaint)
        }

        // 2. Translucent dark navy glass overlay
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(165, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRect(cardRect, tintPaint)

        // Faint street grid texture lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(20, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * s
        }
        var gx = cardRect.left
        while (gx < cardRect.right + cardH) {
            canvas.drawLine(gx, cardRect.top, gx - cardH, cardRect.bottom, gridPaint)
            gx += 40f * s
        }
        canvas.restore()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * s
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

        val pad = 24f * s
        val dividerX = cardRect.left + 245f * s

        // Left Column: FIRDAUS, Time, Date, ASLI Pill (Bigger, bolder fonts)
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 21f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText(cfg.tagLabel.uppercase(Locale.ROOT), cardRect.left + pad, cardRect.top + pad + 18f * s, tagPaint)

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(8f * s, 0f, 3f * s, Color.argb(180, 0, 0, 0))
        }
        canvas.drawText(getDisplayTime(loc), cardRect.left + pad, cardRect.top + pad + 80f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 20f * s
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText(getDisplayDate(loc), cardRect.left + pad, cardRect.top + pad + 118f * s, datePaint)

        // ASLI / Security Pill
        drawSmallShieldPill(canvas, cardRect.left + pad, cardRect.top + pad + 138f * s, sec, "ASLI", s)

        // Vertical divider line
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 255, 255, 255)
            strokeWidth = 1.5f * s
        }
        canvas.drawLine(dividerX, cardRect.top + 18f * s, dividerX, cardRect.bottom - 18f * s, divPaint)

        // Right Column: Address, Coordinates, Signature (Bigger fonts)
        val rightLeft = dividerX + 20f * s
        val rightW = cardRect.right - pad - rightLeft

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 19f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
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
            textSize = 21f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        canvas.drawText("$latStr, $lonStr", rightLeft, cardRect.bottom - pad - 28f * s, coordPaint)

        val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 14f * s
            typeface = Typeface.MONOSPACE
            setShadowLayer(4f * s, 0f, 1f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText("SIG: #${sec.signatureToken} • LIVE ENCRYPTED", rightLeft, cardRect.bottom - pad - 6f * s, sigPaint)
    }

    // ==============================================================
    // DESAIN 2: KARTU ISOMETRIK TERVERIFIKASI (PETA BESAR & JELAS)
    // ==============================================================
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
        val cardH = 265f * s
        val cardRect = RectF(margin, h - margin - cardH, w - margin, h - margin)
        val corner = 22f * s

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 30, 41, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * s
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

        val pad = 22f * s

        // Top Left: FIRDAUS, Time, Date, ASLI TERVERIFIKASI
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 21f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText(cfg.tagLabel.uppercase(Locale.ROOT), cardRect.left + pad, cardRect.top + pad + 18f * s, tagPaint)

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(8f * s, 0f, 3f * s, Color.argb(180, 0, 0, 0))
        }
        canvas.drawText(getDisplayTime(loc), cardRect.left + pad, cardRect.top + pad + 82f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 20f * s
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText(getDisplayDate(loc), cardRect.left + pad, cardRect.top + pad + 118f * s, datePaint)

        drawFilledSecurityPill(canvas, cardRect.left + pad, cardRect.top + pad + 136f * s, sec, "ASLI TERVERIFIKASI", s)

        // Top Right: BIGGER 3D Isometric Map Box
        val mapBoxW = 210f * s
        val mapBoxH = 148f * s
        val mapRect = RectF(cardRect.right - pad - mapBoxW, cardRect.top + pad, cardRect.right - pad, cardRect.top + pad + mapBoxH)
        val mapCorner = 18f * s

        val mapBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(195, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mapRect, mapCorner, mapCorner, mapBoxPaint)

        val mapInnerRect = RectF(mapRect.left + 8f * s, mapRect.top + 6f * s, mapRect.right - 8f * s, mapRect.bottom - 6f * s)
        IsometricMapDrawer.drawIsometricMap(canvas, mapInnerRect, mapTile)

        // Bottom section: Address and Coordinates • SIG (Larger fonts)
        val btmTop = cardRect.top + 188f * s
        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 18f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
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
            color = Color.parseColor("#E2E8F0")
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr   SIG: #${sec.signatureToken}", cardRect.left + pad, cardRect.bottom - pad, subPaint)
    }

    // ==============================================================
    // DESAIN 3: KAPSUL MINIMALIS (PETA & TEKS LEBIH BESAR)
    // ==============================================================
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
        val cardH = 120f * s
        val cardRect = RectF(margin, h - margin - cardH, w - margin, h - margin)
        val corner = cardH / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 30, 41, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * s
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

        // Left circular badge with REAL OpenStreetMap (enlarged)
        val circleRadius = 46f * s
        val circleCx = cardRect.left + 16f * s + circleRadius
        val circleCy = cardRect.centerY()
        val circleRect = RectF(circleCx - circleRadius, circleCy - circleRadius, circleCx + circleRadius, circleCy + circleRadius)

        val circleBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(circleCx, circleCy, circleRadius, circleBg)

        val mapInner = RectF(circleRect.left + 3f * s, circleRect.top + 3f * s, circleRect.right - 3f * s, circleRect.bottom - 3f * s)
        IsometricMapDrawer.draw2DMap(canvas, mapInner, mapTile, isCircle = true)

        // Right circular shield checkmark badge
        val rightRadius = 28f * s
        val rightCx = cardRect.right - 20f * s - rightRadius
        val rightCy = cardRect.centerY()
        drawCheckmarkShieldCircle(canvas, rightCx, rightCy, rightRadius, sec, s)

        // Center Content (Larger text)
        val textLeft = circleCx + circleRadius + 16f * s
        val textRight = rightCx - rightRadius - 14f * s
        val textW = textRight - textLeft

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 19f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText("${cfg.tagLabel.uppercase(Locale.ROOT)} • ", textLeft, cardRect.top + 40f * s, tagPaint)
        val tagW = tagPaint.measureText("${cfg.tagLabel.uppercase(Locale.ROOT)} • ")

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        val timeDateStr = "${getDisplayTime(loc)} • ${getDisplayDate(loc)}"
        canvas.drawText(timeDateStr, textLeft + tagW, cardRect.top + 40f * s, timePaint)

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 16f * s
            setShadowLayer(4f * s, 0f, 1f * s, Color.argb(150, 0, 0, 0))
        }
        val shortAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, Jatirasa, Jatiasih, Kota Bks" }
        val addrLayout = StaticLayout.Builder.obtain(shortAddr, 0, shortAddr.length, addrPaint, textW.toInt())
            .setMaxLines(1).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(textLeft, cardRect.top + 54f * s)
        addrLayout.draw(canvas)
        canvas.restore()

        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 18f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        canvas.drawText("$latStr, $lonStr", textLeft, cardRect.top + 98f * s, coordPaint)
    }

    // ==============================================================
    // DESAIN 4: PETA MELAYANG TAB (TAB BESAR DENGAN PETA NYATA)
    // ==============================================================
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
        val mainCardH = 175f * s
        val tabH = 105f * s
        val tabW = 230f * s
        val gap = 12f * s // Jarak pemisah bersih agar tidak tertabrak card di bawahnya

        val mainRect = RectF(margin, h - margin - mainCardH, w - margin, h - margin)
        // Posisikan tab di pojok kanan atas, melayang bersih DI ATAS card (tanpa menabrak card)
        val tabRight = mainRect.right - 14f * s
        val tabLeft = tabRight - tabW
        val tabBottom = mainRect.top - gap
        val tabTop = tabBottom - tabH
        val tabRect = RectF(tabLeft, tabTop, tabRight, tabBottom)

        // 1. Main Translucent dark card below
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mainRect, 22f * s, 22f * s, mainPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * s
        }
        canvas.drawRoundRect(mainRect, 22f * s, 22f * s, borderPaint)

        // 2. Floating Top-Right Tab with REAL OpenStreetMap (elevated above top-right corner)
        val tabPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(tabRect, 18f * s, 18f * s, tabPaint)

        val tabBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * s
        }
        canvas.drawRoundRect(tabRect, 18f * s, 18f * s, tabBorder)

        IsometricMapDrawer.drawIsometricMap(
            canvas,
            RectF(tabRect.left + 10f * s, tabRect.top + 8f * s, tabRect.right - 10f * s, tabRect.bottom - 8f * s),
            mapTile
        )

        val pad = 22f * s

        // Row 1: FIRDAUS 21:39 • 13 Okt 2026 + ASLI Pill (positioned on left so it doesn't collide with tab)
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 20f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText("${cfg.tagLabel.uppercase(Locale.ROOT)}  ", mainRect.left + pad, mainRect.top + pad + 18f * s, tagPaint)
        val tagW = tagPaint.measureText("${cfg.tagLabel.uppercase(Locale.ROOT)}  ")

        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 21f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        val tStr = "${getDisplayTime(loc)} • ${getDisplayDate(loc)}"
        canvas.drawText(tStr, mainRect.left + pad + tagW, mainRect.top + pad + 18f * s, timePaint)
        val timeW = timePaint.measureText(tStr)

        drawSmallShieldPill(canvas, mainRect.left + pad + tagW + timeW + 14f * s, mainRect.top + pad - 2f * s, sec, "ASLI", s)

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 17f * s
            setShadowLayer(4f * s, 0f, 1f * s, Color.argb(150, 0, 0, 0))
        }
        val fullAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" }
        val addrLayout = StaticLayout.Builder.obtain(fullAddr, 0, fullAddr.length, addrPaint, (mainRect.width() - pad * 2).toInt())
            .setMaxLines(2).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(mainRect.left + pad, mainRect.top + pad + 38f * s)
        addrLayout.draw(canvas)
        canvas.restore()

        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        val btmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 16.5f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr   SIG: #${sec.signatureToken}", mainRect.left + pad, mainRect.bottom - pad + 2f * s, btmPaint)
    }

    // ==============================================================
    // DESAIN 5: PECAH TIGA (PETA SUDUT BESAR & TEKS BESAR)
    // ==============================================================
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
        val corner = 20f * s
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 30, 41, 59)
            style = Paint.Style.FILL
        }

        // 1. Top-Left Card: FIRDAUS, 21:39, 13 Okt 2026 (Bigger)
        val topRect = RectF(margin, margin + 20f * s, margin + 240f * s, margin + 160f * s)
        canvas.drawRoundRect(topRect, corner, corner, bgPaint)

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 19f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText("🛡️ ${cfg.tagLabel.uppercase(Locale.ROOT)}", topRect.left + 18f * s, topRect.top + 34f * s, tagPaint)

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f * s, 0f, 3f * s, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText(getDisplayTime(loc), topRect.left + 18f * s, topRect.top + 88f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 18f * s
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText(getDisplayDate(loc), topRect.left + 18f * s, topRect.top + 120f * s, datePaint)

        // 2. Bottom-Right Card: BIGGER Real OpenStreetMap
        val mapSize = 160f * s
        val mapRect = RectF(w - margin - mapSize, h - margin - mapSize, w - margin, h - margin)
        val mapBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(195, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mapRect, corner, corner, mapBoxPaint)
        IsometricMapDrawer.drawIsometricMap(canvas, RectF(mapRect.left + 10f * s, mapRect.top + 10f * s, mapRect.right - 10f * s, mapRect.bottom - 10f * s), mapTile)

        // 3. Bottom-Left Card: Address & Coordinates
        val addrW = mapRect.left - margin - 16f * s
        val btmLeftRect = RectF(margin, h - margin - mapSize, margin + addrW, h - margin)
        canvas.drawRoundRect(btmLeftRect, corner, corner, bgPaint)

        val pad = 18f * s
        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 17f * s
            setShadowLayer(4f * s, 0f, 1f * s, Color.argb(150, 0, 0, 0))
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
            textSize = 19f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr", btmLeftRect.left + pad, btmLeftRect.bottom - pad + 2f * s, coordPaint)
    }

    // ==============================================================
    // DESAIN 6: KARTU PUTIH ELEGAN (PETA BESAR & TEKS BESAR)
    // ==============================================================
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
        val cardH = 200f * s
        val cardRect = RectF(margin, h - margin - cardH, w - margin, h - margin)
        val corner = 22f * s

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(175, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(50, 15, 23, 42)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * s
        }
        canvas.drawRoundRect(cardRect, corner, corner, borderPaint)

        // Left Side: ENLARGED REAL OpenStreetMap Box
        val mapBoxW = 180f * s
        val mapRect = RectF(cardRect.left + 16f * s, cardRect.top + 16f * s, cardRect.left + 16f * s + mapBoxW, cardRect.bottom - 16f * s)
        val mapInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 241, 245, 249)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mapRect, 16f * s, 16f * s, mapInnerPaint)
        IsometricMapDrawer.drawIsometricMap(canvas, RectF(mapRect.left + 8f * s, mapRect.top + 8f * s, mapRect.right - 8f * s, mapRect.bottom - 8f * s), mapTile)

        // Right Side Content (Bigger)
        val rightLeft = mapRect.right + 20f * s
        val rightW = cardRect.right - 20f * s - rightLeft

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 21f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${cfg.tagLabel.uppercase(Locale.ROOT)}   ", rightLeft, cardRect.top + 42f * s, tagPaint)
        val tagW = tagPaint.measureText("${cfg.tagLabel.uppercase(Locale.ROOT)}   ")

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 26f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(getDisplayTime(loc), rightLeft + tagW, cardRect.top + 42f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 17f * s
        }
        canvas.drawText(getDisplayDate(loc), rightLeft, cardRect.top + 68f * s, datePaint)

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 16f * s
        }
        val fullAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, RT.004/RW.002, Jatirasa, Kec. Jatiasih, Kota Bks, Jawa Barat 17424" }
        val addrLayout = StaticLayout.Builder.obtain(fullAddr, 0, fullAddr.length, addrPaint, rightW.toInt())
            .setMaxLines(2).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(rightLeft, cardRect.top + 80f * s)
        addrLayout.draw(canvas)
        canvas.restore()

        val isTampered = !sec.isAuthentic || sec.isMockDetected || sec.isTimeManipulated
        val secTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.parseColor("#EF4444") else Color.parseColor("#059669")
            textSize = 16f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val secLabel = if (isTampered) "⚠️ FAKE GPS / TAMPERED" else "✓ Asli terverifikasi"
        canvas.drawText(secLabel, rightLeft, cardRect.bottom - 20f * s, secTextPaint)

        val tokenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 16f * s
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText("#${sec.signatureToken}", cardRect.right - 22f * s - tokenPaint.measureText("#${sec.signatureToken}"), cardRect.bottom - 20f * s, tokenPaint)
    }

    // ==============================================================
    // DESAIN 7: STRIP VERTIKAL KIRI (PETA BESAR & NOMOR JAM BESAR)
    // ==============================================================
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
            color = Color.argb(145, 30, 41, 59)
            style = Paint.Style.FILL
        }

        // Left Vertical Pillar (Enlarged)
        val stripW = 145f * s
        val stripRect = RectF(margin, margin + 40f * s, margin + stripW, h - margin)
        canvas.drawRoundRect(stripRect, corner, corner, bgPaint)

        // Top inside strip: REAL OpenStreetMap
        val mapBoxSize = stripW - 22f * s
        val mapRect = RectF(stripRect.left + 11f * s, stripRect.top + 14f * s, stripRect.right - 11f * s, stripRect.top + 14f * s + mapBoxSize)
        val mapBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(195, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(mapRect, 16f * s, 16f * s, mapBg)
        IsometricMapDrawer.drawIsometricMap(canvas, RectF(mapRect.left + 6f * s, mapRect.top + 6f * s, mapRect.right - 6f * s, mapRect.bottom - 6f * s), mapTile)

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 18f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText(cfg.tagLabel.uppercase(Locale.ROOT), stripRect.centerX(), mapRect.bottom + 32f * s, tagPaint)

        // Stacked numbers: HH on top, mm below
        val timeParts = getDisplayTime(loc).split(":")
        val hour = if (timeParts.isNotEmpty()) timeParts[0] else SimpleDateFormat("HH", Locale.ROOT).format(Date())
        val min = if (timeParts.size > 1) timeParts[1] else SimpleDateFormat("mm", Locale.ROOT).format(Date())

        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText(hour, stripRect.centerX(), mapRect.bottom + 82f * s, numPaint)
        canvas.drawText(min, stripRect.centerX(), mapRect.bottom + 132f * s, numPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 17f * s
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        val dateText = getDisplayDate(loc).replace(" 2026", "")
        canvas.drawText(dateText, stripRect.centerX(), mapRect.bottom + 164f * s, datePaint)

        drawCheckmarkShieldCircle(canvas, stripRect.centerX(), stripRect.bottom - 42f * s, 26f * s, sec, s)

        // Right Bottom Info Card
        val infoLeft = stripRect.right + 16f * s
        val infoCardH = 160f * s
        val infoRect = RectF(infoLeft, h - margin - infoCardH, w - margin, h - margin)
        canvas.drawRoundRect(infoRect, 20f * s, 20f * s, bgPaint)

        val pad = 20f * s
        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 17f * s
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
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
            textSize = 20f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr", infoRect.left + pad, infoRect.bottom - pad - 26f * s, coordPaint)

        val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 15f * s
            typeface = Typeface.MONOSPACE
            setShadowLayer(3f * s, 0f, 1f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText("SIG: #${sec.signatureToken} • LIVE ENCRYPTED", infoRect.left + pad, infoRect.bottom - pad, sigPaint)
    }

    // ==============================================================
    // DESAIN 8: SEGEL KRIPTOGRAFIS (STEMPEL & PETA BESAR)
    // ==============================================================
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
        val cardH = 180f * s
        val sealRadius = 80f * s
        val sealCx = margin + sealRadius
        val sealCy = h - margin - cardH / 2f

        drawCryptoSealStamp(canvas, sealCx, sealCy, sealRadius, s, mapTile, sec)

        val cardLeft = sealCx + sealRadius + 16f * s
        val cardRect = RectF(cardLeft, h - margin - cardH, w - margin, h - margin)
        val corner = 22f * s

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 30, 41, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, corner, corner, bgPaint)

        val pad = 20f * s

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 20f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText("${cfg.tagLabel.uppercase(Locale.ROOT)}   ", cardRect.left + pad, cardRect.top + pad + 18f * s, tagPaint)
        val tagW = tagPaint.measureText("${cfg.tagLabel.uppercase(Locale.ROOT)}   ")

        val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(5f * s, 0f, 2f * s, Color.argb(160, 0, 0, 0))
        }
        canvas.drawText(getDisplayTime(loc), cardRect.left + pad + tagW, cardRect.top + pad + 18f * s, clockPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 17f * s
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText(getDisplayDate(loc), cardRect.left + pad, cardRect.top + pad + 44f * s, datePaint)

        val addrPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F8FAFC")
            textSize = 16.5f * s
            setShadowLayer(4f * s, 0f, 1f * s, Color.argb(150, 0, 0, 0))
        }
        val shortAddr = loc.address.ifEmpty { "Gg. Kuncir No.27, Jatirasa, Jatiasih, Kota Bks" }
        val addrLayout = StaticLayout.Builder.obtain(shortAddr, 0, shortAddr.length, addrPaint, (cardRect.width() - pad * 2).toInt())
            .setMaxLines(2).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(cardRect.left + pad, cardRect.top + pad + 56f * s)
        addrLayout.draw(canvas)
        canvas.restore()

        val latStr = String.format(Locale.US, "%.6f", loc.latitude).replace('.', ',')
        val lonStr = String.format(Locale.US, "%.6f", loc.longitude).replace('.', ',')
        val btmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 16.5f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f * s, 0f, 2f * s, Color.argb(150, 0, 0, 0))
        }
        canvas.drawText("$latStr, $lonStr   #${sec.signatureToken}", cardRect.left + pad, cardRect.bottom - pad + 2f * s, btmPaint)
    }

    // ==========================================
    // HELPER BADGES & SEALS
    // ==========================================
    private fun getDisplayTime(loc: LiveLocationData): String {
        return loc.formattedTime.ifEmpty {
            SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date())
        }
    }

    private fun getDisplayDate(loc: LiveLocationData): String {
        return loc.formattedDate.ifEmpty {
            SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
        }
    }

    private fun drawSmallShieldPill(canvas: Canvas, x: Float, y: Float, sec: SecurityCheckResult, defaultText: String, s: Float) {
        val isTampered = !sec.isAuthentic || sec.isMockDetected || sec.isTimeManipulated
        val text = when {
            sec.isMockDetected && sec.isTimeManipulated -> "⚠️ FAKE GPS & WAKTU"
            sec.isMockDetected -> "⚠️ FAKE GPS"
            sec.isTimeManipulated -> "⚠️ WAKTU PALSU"
            else -> "🛡️ $defaultText"
        }
        val strokeColor = if (isTampered) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
        val textColor = if (isTampered) Color.parseColor("#FCA5A5") else Color.parseColor("#34D399")
        val bgColor = if (isTampered) Color.argb(210, 127, 29, 29) else Color.TRANSPARENT

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 14f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textW = textPaint.measureText(text)
        val pillRect = RectF(x, y, x + textW + 16f * s, y + 26f * s)

        if (bgColor != Color.TRANSPARENT) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgColor
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(pillRect, 13f * s, 13f * s, bgPaint)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * s
        }
        canvas.drawRoundRect(pillRect, 13f * s, 13f * s, paint)
        canvas.drawText(text, x + 8f * s, y + 18.5f * s, textPaint)
    }

    private fun drawFilledSecurityPill(canvas: Canvas, x: Float, y: Float, sec: SecurityCheckResult, defaultText: String, s: Float) {
        val isTampered = !sec.isAuthentic || sec.isMockDetected || sec.isTimeManipulated
        val text = when {
            sec.isMockDetected && sec.isTimeManipulated -> "⚠️ FAKE GPS & WAKTU"
            sec.isMockDetected -> "⚠️ FAKE GPS"
            sec.isTimeManipulated -> "⚠️ WAKTU PALSU"
            else -> "🛡️ $defaultText"
        }
        val bgColor = if (isTampered) Color.argb(220, 127, 29, 29) else Color.argb(210, 6, 78, 59)
        val textColor = if (isTampered) Color.parseColor("#FCA5A5") else Color.parseColor("#6EE7B7")

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 14f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textW = textPaint.measureText(text)
        val pillRect = RectF(x, y, x + textW + 18f * s, y + 28f * s)
        canvas.drawRoundRect(pillRect, 14f * s, 14f * s, bgPaint)
        canvas.drawText(text, x + 9f * s, y + 19.5f * s, textPaint)
    }

    private fun drawCheckmarkShieldCircle(canvas: Canvas, cx: Float, cy: Float, r: Float, sec: SecurityCheckResult, s: Float) {
        val isTampered = !sec.isAuthentic || sec.isMockDetected || sec.isTimeManipulated
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.argb(220, 127, 29, 29) else Color.argb(175, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r, circlePaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * s
        }
        canvas.drawCircle(cx, cy, r, strokePaint)

        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.parseColor("#FCA5A5") else Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 2.8f * s
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        if (isTampered) {
            canvas.drawLine(cx, cy - r * 0.45f, cx, cy + r * 0.1f, checkPaint)
            canvas.drawPoint(cx, cy + r * 0.45f, checkPaint.apply { strokeWidth = 4.5f * s })
        } else {
            val checkPath = Path().apply {
                moveTo(cx - 9f * s, cy - 1f * s)
                lineTo(cx - 2f * s, cy + 7f * s)
                lineTo(cx + 9f * s, cy - 7f * s)
            }
            canvas.drawPath(checkPath, checkPaint)
        }
    }

    private fun drawCryptoSealStamp(canvas: Canvas, cx: Float, cy: Float, r: Float, s: Float, mapTile: Bitmap?, sec: SecurityCheckResult) {
        val isTampered = !sec.isAuthentic || sec.isMockDetected || sec.isTimeManipulated
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.argb(230, 69, 10, 10) else Color.argb(175, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, r, bgPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * s
        }
        canvas.drawCircle(cx, cy, r - 2f * s, ringPaint)

        // Inner circle with REAL OpenStreetMap
        val innerRadius = r * 0.65f
        val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 248, 250, 252)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, innerRadius, innerCirclePaint)

        val mapRect = RectF(cx - innerRadius + 2f * s, cy - innerRadius + 2f * s, cx + innerRadius - 2f * s, cy + innerRadius - 2f * s)
        IsometricMapDrawer.draw2DMap(canvas, mapRect, mapTile, isCircle = true)

        val textPath = Path().apply {
            addCircle(cx, cy, r - 14f * s, Path.Direction.CW)
        }
        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isTampered) Color.parseColor("#FCA5A5") else Color.parseColor("#6EE7B7")
            textSize = 11.5f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.18f
        }
        val stampText = if (isTampered) "⚠️ FAKE GPS / TAMPERED • BAHAYA •" else "LIVE ENCRYPTED • ASLI TERVERIFIKASI •"
        canvas.drawTextOnPath(stampText, textPath, 0f, 0f, stampTextPaint)
    }
}
