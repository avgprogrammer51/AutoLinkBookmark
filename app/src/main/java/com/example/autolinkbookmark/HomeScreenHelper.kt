package com.example.autolinkbookmark

import android.accessibilityservice.AccessibilityService
import android.graphics.Path
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class HomeScreenHelper(private val service: AccessibilityService) {
    
    companion object {
        private const val TAG = "HomeScreenHelper"
    }
    
    fun getAllShortcuts(): List<Pair<String, AccessibilityNodeInfo>> {
        val shortcuts = mutableListOf<Pair<String, AccessibilityNodeInfo>>()
        try {
            val root = service.rootInActiveWindow ?: return shortcuts
            collectClickableShortcuts(root, shortcuts)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting shortcuts: ${e.message}")
        }
        return shortcuts
    }
    
    private fun collectClickableShortcuts(
        node: AccessibilityNodeInfo?,
        result: MutableList<Pair<String, AccessibilityNodeInfo>>
    ) {
        if (node == null) return
        try {
            if (node.isClickable && node.isEnabled) {
                val label = node.contentDescription?.toString() 
                    ?: node.text?.toString() 
                    ?: "Unknown"
                
                if (label != "Unknown" && !result.any { it.first == label }) {
                    result.add(Pair(label, node))
                    Log.d(TAG, "Found shortcut: $label")
                }
            }
            
            for (i in 0 until (node.childCount ?: 0)) {
                collectClickableShortcuts(node.getChild(i), result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in recursion: ${e.message}")
        }
    }
    
    fun clickNode(node: AccessibilityNodeInfo): Boolean {
        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking node: ${e.message}")
            false
        }
    }
    
    fun getChromeCurrentUrl(): String {
        return try {
            val root = service.rootInActiveWindow ?: return ""
            findAddressBarText(root)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Chrome URL: ${e.message}")
            ""
        }
    }
    
    private fun findAddressBarText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        try {
            val text = node.text?.toString() ?: ""
            if (text.startsWith("http://") || text.startsWith("https://")) {
                return text
            }
            
            val desc = node.contentDescription?.toString() ?: ""
            if (desc.startsWith("http://") || desc.startsWith("https://")) {
                return desc
            }
            
            for (i in 0 until (node.childCount ?: 0)) {
                val result = findAddressBarText(node.getChild(i))
                if (result.isNotEmpty()) return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching nodes: ${e.message}")
        }
        return ""
    }
}
