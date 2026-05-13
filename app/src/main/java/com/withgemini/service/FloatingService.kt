package com.withgemini.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var webView: WebView
    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 1. Transparent WebView Banana (Bina Background Ke)
        webView = WebView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true // JS zaroori hai
            settings.domStorageEnabled = true // LocalStorage save karne ke liye
            settings.allowFileAccess = true   // Assets folder read karne ke liye
            
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            
            // 2. The Magic Bridge: Kotlin aur HTML ka connection
            addJavascriptInterface(WebAppInterface(this@FloatingService, this@FloatingService), "AndroidBridge")
            
            // 3. Tera HTML File Load Karna
            loadUrl("file:///android_asset/public/index.html")
        }

        // Window Permissions aur Design
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // HTML mein dragging aur panels hain, isliye MATCH_PARENT diya hai
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(webView, layoutParams)
    }

    private fun startForegroundNotification() {
        val channelId = "akira_bridge_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Akira Master Engine", NotificationManager.IMPORTANCE_MIN)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Akira Engine Online")
            .setContentText("The Hacker WebView is active.")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System ka default icon
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Safai: Agar app band ho toh WebView ko screen se nikal do
        if (::webView.isInitialized) {
            windowManager.removeView(webView)
        }
    }
}
