package com.example.hack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class ContactFragment : Fragment() {

    private val REQUEST_CODE_CONTACTS = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if the contacts permission is granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CODE_CONTACTS)
        } else {
            // Permission already granted, start the file transfer service
            startFileTransferService()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                startFileTransferService()
            } else {
                // Permission denied
                Toast.makeText(requireContext(), "Contacts permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFileTransferService() {
        // Start your FileTransferService here
        val intent = Intent(requireContext(), FileTransferService::class.java)
        requireContext().startService(intent)
    }
}
