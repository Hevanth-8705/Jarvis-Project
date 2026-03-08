package com.example.jarvisandroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnStart: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            checkOverlayPermission()
        } else {
            statusText.text = "Error: Permissions denied."
            Toast.makeText(this, "Microphone access is required for Jarvis.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        btnStart = findViewById(R.id.btnStart)

        btnStart.setOnClickListener {
            checkAndRequestPermissions()
        }

        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (areBasePermissionsGranted() && Settings.canDrawOverlays(this)) {
            statusText.text = "Jarvis is ready."
            btnStart.visibility = View.VISIBLE
            btnStart.text = "Start Jarvis Agent"
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isEmpty()) {
            checkOverlayPermission()
        } else {
            statusText.text = "Requesting permissions..."
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun areBasePermissionsGranted(): Boolean {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            statusText.text = "Action Required: Enable Overlay"
            Toast.makeText(this, "Please allow Jarvis to 'Display over other apps'", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(intent)
        } else {
            startFloatingAssistant()
        }
    }

    private fun startFloatingAssistant() {
        statusText.text = "Jarvis systems active."
        val intent = Intent(this, FloatingService::class.java)
        ContextCompat.startForegroundService(this, intent)
        // Keep the activity open for a moment so the user sees success
        btnStart.visibility = View.VISIBLE
        btnStart.text = "Re-Sync Systems"
    }
}
