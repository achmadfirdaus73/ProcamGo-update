package com.example.watermark

import android.graphics.*

object IsometricMapDrawer {

    fun drawIsometricMap(canvas: Canvas, rect: RectF, mapTile: Bitmap?) {
        val w = rect.width()
        val h = rect.height()
        val cx = rect.centerX()
        val cy = rect.centerY() + h * 0.05f

        val scale = w / 200f

        val slabDx = 76f * scale
        val slabDy = 40f * scale
        val thickness = 14f * scale

        val pTop = PointF(cx, cy - slabDy)
        val pRight = PointF(cx + slabDx, cy)
        val pBottom = PointF(cx, cy + slabDy)
        val pLeft = PointF(cx - slabDx, cy)

        // 1. 3D extrusion side faces
        val leftFacePath = Path().apply {
            moveTo(pLeft.x, pLeft.y)
            lineTo(pBottom.x, pBottom.y)
            lineTo(pBottom.x, pBottom.y + thickness)
            lineTo(pLeft.x, pLeft.y + thickness)
            close()
        }
        val leftFacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.FILL
        }
        canvas.drawPath(leftFacePath, leftFacePaint)

        val rightFacePath = Path().apply {
            moveTo(pBottom.x, pBottom.y)
            lineTo(pRight.x, pRight.y)
            lineTo(pRight.x, pRight.y + thickness)
            lineTo(pBottom.x, pBottom.y + thickness)
            close()
        }
        val rightFacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            style = Paint.Style.FILL
        }
        canvas.drawPath(rightFacePath, rightFacePaint)

        // 2. Top Diamond Surface
        val topFacePath = Path().apply {
            moveTo(pTop.x, pTop.y)
            lineTo(pRight.x, pRight.y)
            lineTo(pBottom.x, pBottom.y)
            lineTo(pLeft.x, pLeft.y)
            close()
        }

        // Draw Real OpenStreetMap Tile clipped inside the diamond
        canvas.save()
        canvas.clipPath(topFacePath)

        if (mapTile != null && !mapTile.isRecycled) {
            val mapRect = RectF(pLeft.x - 10f * scale, pTop.y - 10f * scale, pRight.x + 10f * scale, pBottom.y + 10f * scale)
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(mapTile, null, mapRect, bmpPaint)
        } else {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.FILL
            }
            canvas.drawPath(topFacePath, bgPaint)
        }
        canvas.restore()

        // Border around diamond
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 148, 163, 184)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }
        canvas.drawPath(topFacePath, borderPaint)

        // 3. Central Red Location Pin Marker on real map
        val pinX = cx
        val pinTipY = cy + 4f * scale
        val pinRadius = 13f * scale
        val pinCenterY = pinTipY - 26f * scale

        // Pin shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawOval(
            RectF(pinX - 10f * scale, pinTipY - 3f * scale, pinX + 10f * scale, pinTipY + 4f * scale),
            shadowPaint
        )

        // Red Pin Body
        val pinPath = Path().apply {
            arcTo(
                RectF(pinX - pinRadius, pinCenterY - pinRadius, pinX + pinRadius, pinCenterY + pinRadius),
                -150f,
                300f,
                false
            )
            lineTo(pinX, pinTipY)
            close()
        }
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EF4444")
            style = Paint.Style.FILL
        }
        canvas.drawPath(pinPath, pinPaint)

        val pinStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B91C1C")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * scale
        }
        canvas.drawPath(pinPath, pinStrokePaint)

        // White dot inside pin
        val pinDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(pinX, pinCenterY, 4.5f * scale, pinDotPaint)
    }

    fun draw2DMap(canvas: Canvas, rect: RectF, mapTile: Bitmap?, cornerRadius: Float = 0f, isCircle: Boolean = false) {
        val scale = rect.width() / 100f

        canvas.save()
        if (isCircle) {
            val circlePath = Path().apply {
                addCircle(rect.centerX(), rect.centerY(), rect.width() / 2f, Path.Direction.CW)
            }
            canvas.clipPath(circlePath)
        } else if (cornerRadius > 0f) {
            val roundPath = Path().apply {
                addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            canvas.clipPath(roundPath)
        }

        if (mapTile != null && !mapTile.isRecycled) {
            val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(mapTile, null, rect, bmpPaint)
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.FILL
            }
            canvas.drawRect(rect, fallbackPaint)
        }
        canvas.restore()

        // Red location marker in center
        val pinX = rect.centerX()
        val pinTipY = rect.centerY() + 6f * scale
        val pinRadius = 9f * scale
        val pinCenterY = pinTipY - 18f * scale

        val pinPath = Path().apply {
            arcTo(RectF(pinX - pinRadius, pinCenterY - pinRadius, pinX + pinRadius, pinCenterY + pinRadius), -150f, 300f, false)
            lineTo(pinX, pinTipY)
            close()
        }
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EF4444")
            style = Paint.Style.FILL
        }
        canvas.drawPath(pinPath, pinPaint)

        val pinDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(pinX, pinCenterY, 3.5f * scale, pinDotPaint)
    }
}
