package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AegisBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i("AegisBootReceiver", "Boot completed detected. Restoring Aegis background assistant.")
            AegisBackgroundService.startService(context)
        }
    }
}
