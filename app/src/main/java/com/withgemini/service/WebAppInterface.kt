package com.withgemini.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class WebAppInterface(private val context: Context, private val service: FloatingService) {

    // 1. Gemini Launch karna
    @JavascriptInterface
    fun launchGemini() {
        val pm = context.packageManager
        // Pehle check karega Gemini (Bard) package hai ya nahi
        var intent = pm.getLaunchIntentForPackage("com.google.android.apps.bard")
        if (intent == null) {
            // Agar Gemini nahi mila toh Assistant khol dega
            intent = Intent(Intent.ACTION_VOICE_COMMAND)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice Assistant missing!", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Custom App Launch karna (App Picker se)
    @JavascriptInterface
    fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "App not found!", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. SOS Vibrator (Jab Timer 0 pe hit hoga)
    @JavascriptInterface
    fun triggerSOSVibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        // 3 baar khatarnak jhatka: Off 0ms, On 500ms, Off 200ms...
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    // 4. Kill Switch (Float button disable karne pe)
    @JavascriptInterface
    fun killOverlay() {
        service.stopSelf() // Pura system kill
    }

    // 5. Mobile mein install apps ki list nikalna (JSON format mein return)
    @JavascriptInterface
    fun getInstalledApps(): String {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.category.LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
        val jsonArray = JSONArray()
        
        apps.forEach { resolveInfo ->
            val jsonObject = JSONObject()
            jsonObject.put("name", resolveInfo.loadLabel(pm).toString())
            jsonObject.put("packageName", resolveInfo.activityInfo.packageName)
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
}
