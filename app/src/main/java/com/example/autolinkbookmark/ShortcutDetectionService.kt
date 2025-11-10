package com.example.autolinkbookmark

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class ShortcutDetectionService : AccessibilityService() {
    
    companion object {
        const val TAG = "ShortcutDetector"
        var isRunning = false
        var callback: ((String) -> Unit)? = null
        var progressCallback: ((Int, Int, String) -> Unit)? = null
    }
    
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val helper by lazy { HomeScreenHelper(this) }
    private var shortcuts = mutableListOf<Pair<String, String>>()
    private var currentIndex = 0
    private var isProcessing = false
    
    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_TREE
            notificationTimeout = 100
        }
        setServiceInfo(info)
        Log.d(TAG, "Service connected")
        isRunning = true
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: ""
                
                if (packageName == "com.android.chrome" && isProcessing) {
                    scope.launch {
                        delay(2500)
                        processCurrentUrl()
                    }
                }
                
                if ((packageName.contains("launcher") || packageName == "com.android.systemui") 
                    && isProcessing) {
                    scope.launch {
                        delay(1000)
                        processNextShortcut()
                    }
                }
            }
        }
    }
    
    fun startExtraction() {
        if (isProcessing) return
        isProcessing = true
        scope.launch {
            try {
                callback?.invoke("Detecting shortcuts...")
                performGlobalAction(GLOBAL_ACTION_HOME)
                delay(1000)
                
                val detectedShortcuts = helper.getAllShortcuts()
                Log.d(TAG, "Found ${detectedShortcuts.size} shortcuts")
                callback?.invoke("Found ${detectedShortcuts.size} shortcuts")
                
                shortcuts.clear()
                for ((label, _) in detectedShortcuts) {
                    shortcuts.add(Pair(label, ""))
                }
                
                delay(1000)
                currentIndex = 0
                processNextShortcut()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                callback?.invoke("Error: ${e.message}")
                isProcessing = false
            }
        }
    }
    
    private suspend fun processNextShortcut() {
        if (currentIndex >= shortcuts.size) {
            callback?.invoke("✓ Completed! Processed ${shortcuts.size}")
            isProcessing = false
            currentIndex = 0
            return
        }
        
        try {
            val (label, _) = shortcuts[currentIndex]
            val progress = "${currentIndex + 1}/${shortcuts.size}"
            
            progressCallback?.invoke(currentIndex, shortcuts.size, label)
            callback?.invoke("[$progress] Opening: $label")
            
            val allShortcuts = helper.getAllShortcuts()
            val targetShortcut = allShortcuts.find { it.first == label }
            
            if (targetShortcut != null) {
                helper.clickNode(targetShortcut.second)
            } else {
                currentIndex++
                delay(500)
                processNextShortcut()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            currentIndex++
            delay(500)
            processNextShortcut()
        }
    }
    
    private suspend fun processCurrentUrl() {
        try {
            val url = helper.getChromeCurrentUrl()
            
            if (url.isNotEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
                shortcuts[currentIndex] = Pair(shortcuts[currentIndex].first, url)
                saveChromeBookmark(url)
                delay(1000)
            }
            
            performGlobalAction(GLOBAL_ACTION_BACK)
            currentIndex++
            delay(1500)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            currentIndex++
        }
    }
    
    private suspend fun saveChromeBookmark(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("chrome://bookmarks/add?url=$url")
                setPackage("com.android.chrome")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Log.d(TAG, "Bookmark saved: $url")
            delay(1500)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving: ${e.message}")
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isProcessing = false
        job.cancel()
    }
    
    fun stopExtraction() {
        isProcessing = false
        currentIndex = shortcuts.size
    }
}
