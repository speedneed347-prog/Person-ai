package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AutomationStep(
    val actionType: String, // CLICK_TEXT, CLICK_ID, INPUT_TEXT, OPEN_APP, GLOBAL_ACTION
    val target: String,
    val payload: String? = null,
    val isSensitive: Boolean = false
)

data class AutomationReport(
    val success: Boolean,
    val executedSteps: Int,
    val summary: String
)

class AegisAccessibilityService : AccessibilityService() {

    private val tag = "AegisAccessibility"

    companion object {
        var instance: AegisAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(tag, "Aegis Accessibility Automation Service Connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active window event tracking
    }

    override fun onInterrupt() {
        Log.w(tag, "Aegis Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /**
     * Finds and clicks an interactive node by visible text label.
     */
    fun findAndClickByText(text: String, exact: Boolean = false): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text) ?: return false

        for (node in nodes) {
            val nodeText = node.text?.toString() ?: ""
            val matches = if (exact) nodeText.equals(text, ignoreCase = true) else nodeText.contains(text, ignoreCase = true)
            if (matches) {
                var targetNode: AccessibilityNodeInfo? = node
                while (targetNode != null && !targetNode.isClickable) {
                    targetNode = targetNode.parent
                }
                if (targetNode != null && targetNode.isClickable) {
                    val clicked = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d(tag, "Clicked node with text: $text (success: $clicked)")
                    return clicked
                }
            }
        }
        return false
    }

    /**
     * Finds and clicks a node by View Resource ID.
     */
    fun findAndClickById(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId) ?: return false

        for (node in nodes) {
            var targetNode: AccessibilityNodeInfo? = node
            while (targetNode != null && !targetNode.isClickable) {
                targetNode = targetNode.parent
            }
            if (targetNode != null && targetNode.isClickable) {
                return targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    /**
     * Types text into a focused or target text field.
     */
    fun findAndSetText(text: String, targetLabel: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false

        if (targetLabel != null) {
            val nodes = root.findAccessibilityNodeInfosByText(targetLabel)
            val inputNode = nodes?.firstOrNull { it.isEditable }
            if (inputNode != null) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                return inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }

        // Fall back to currently focused input node
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        return false
    }

    /**
     * Reads all text content hierarchy from the active foreground window.
     */
    fun readScreenContent(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val textList = mutableListOf<String>()
        traverseNodeHierarchy(root, textList)
        return textList
    }

    private fun traverseNodeHierarchy(node: AccessibilityNodeInfo?, output: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrEmpty() && text.length > 1) {
            output.add(text)
        } else if (!desc.isNullOrEmpty() && desc.length > 1) {
            output.add(desc)
        }

        for (i in 0 until node.childCount) {
            traverseNodeHierarchy(node.getChild(i), output)
        }
    }

    fun triggerGlobalBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun triggerGlobalHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun triggerGlobalRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun triggerGlobalNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    /**
     * Executes a multi-step automation workflow.
     */
    suspend fun executeWorkflow(
        steps: List<AutomationStep>,
        ownerApproved: Boolean = false
    ): AutomationReport = withContext(Dispatchers.Default) {
        var count = 0

        for (step in steps) {
            if (step.isSensitive && !ownerApproved) {
                return@withContext AutomationReport(
                    success = false,
                    executedSteps = count,
                    summary = "Workflow paused: Sensitive step requires owner biometric authorization."
                )
            }

            val stepSuccess = when (step.actionType) {
                "CLICK_TEXT" -> findAndClickByText(step.target)
                "CLICK_ID" -> findAndClickById(step.target)
                "INPUT_TEXT" -> findAndSetText(step.payload ?: "", step.target)
                "GLOBAL_BACK" -> triggerGlobalBack()
                "GLOBAL_HOME" -> triggerGlobalHome()
                else -> false
            }

            if (stepSuccess) count++
            kotlinx.coroutines.delay(600) // Small cadence between UI actions
        }

        AutomationReport(
            success = count > 0,
            executedSteps = count,
            summary = "Completed $count of ${steps.size} automation steps."
        )
    }
}
