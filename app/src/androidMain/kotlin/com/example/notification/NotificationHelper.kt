package com.example.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NotificationHelper {

    private const val CHANNEL_ID = "photo_save_and_sync"
    private const val CHANNEL_NAME = "Foto & Sinkronisasi"
    private const val CHANNEL_DESC = "Notifikasi saat foto disimpan ke galeri dan tersinkronisasi ke Spreadsheet"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    suspend fun showPhotoSavedNotification(
        context: Context,
        fileName: String,
        address: String
    ) {
        initNotificationChannel(context)

        // 1. In-App Toast agar pengguna langsung melihat konfirmasi seketika di layar
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "📸 Foto '$fileName' berhasil disimpan ke Galeri!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 2. Android System Notification di status bar / drawer
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            val iconRes = try {
                R.mipmap.ic_launcher
            } catch (_: Exception) {
                android.R.drawable.ic_menu_camera
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle("📸 Foto Berhasil Disimpan")
                .setContentText("$fileName telah tersimpan di Galeri & Penyimpanan Lokal")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("File: $fileName\nLokasi: $address\nStatus: Tersimpan aman di Galeri perangkat.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val notificationId = (System.currentTimeMillis() % 100000).toInt() + 1000
            val manager = NotificationManagerCompat.from(context)
            @SuppressLint("MissingPermission")
            manager.notify(notificationId, notification)
        } catch (_: Exception) {
            // Abaikan jika notifikasi dinonaktifkan oleh pengguna di sistem
        }
    }

    suspend fun showPhotoSyncedNotification(
        context: Context,
        fileName: String,
        target: String = "Google Spreadsheet"
    ) {
        initNotificationChannel(context)

        // 1. In-App Toast
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "☁️ Foto '$fileName' berhasil disinkronkan ke $target!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 2. Android System Notification
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            val iconRes = try {
                R.mipmap.ic_launcher
            } catch (_: Exception) {
                android.R.drawable.stat_sys_upload_done
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle("☁️ Foto Berhasil Disinkronkan")
                .setContentText("$fileName berhasil tersinkronisasi ke $target")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Data foto '$fileName' dan koordinat GPS telah terunggah ke $target.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val notificationId = (System.currentTimeMillis() % 100000).toInt() + 2000
            val manager = NotificationManagerCompat.from(context)
            @SuppressLint("MissingPermission")
            manager.notify(notificationId, notification)
        } catch (_: Exception) {
            // Abaikan jika notifikasi dinonaktifkan di sistem
        }
    }
}
