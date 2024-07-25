package com.example.hack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the button from the layout
        val startButton: Button = findViewById(R.id.startButton)

        // Set an OnClickListener on the button
        startButton.setOnClickListener {
            // Create an intent to start the FileTransferService
            val intent = Intent(this, FileTransferService::class.java)

            // Start the service
            startService(intent)
        }
    }
}
