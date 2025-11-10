package com.example.autolinkbookmark

import android.content.Intent
import android.content.ComponentName
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        
        startButton.setOnClickListener { startExtraction() }
        stopButton.setOnClickListener { stopExtraction() }
        
        ShortcutDetectionService.callback = { message ->
            runOnUiThread {
                statusText.text = message
            }
        }
        
        ShortcutDetectionService.progressCallback = { current, total, label ->
            runOnUiThread {
                progressBar.max = total
                progressBar.progress = current
            }
        }
        
        if (!isAccessibilityEnabled()) {
            showAccessibilityDialog()
        }
    }
    
    private fun startExtraction() {
        if (!ShortcutDetectionService.isRunning) {
            statusText.text = "Enable accessibility first!"
            showAccessibilityDialog()
            return
        }
        
        startButton.isEnabled = false
        statusText.text = "Starting..."
        ShortcutDetectionService().startExtraction()
    }
    
    private fun stopExtraction() {
        ShortcutDetectionService().stopExtraction()
        startButton.isEnabled = true
        statusText.text = "Stopped"
    }
    
    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        
        val componentName = ComponentName(this, ShortcutDetectionService::class.java).flattenToString()
        return enabledServices.contains(componentName)
    }
    
    private fun showAccessibilityDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Enable Accessibility")
            .setMessage("Settings > Accessibility > AutoLinkBookmark > Enable")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
