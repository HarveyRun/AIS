package com.shixianwen.shixianwen_mobile

import android.content.Intent
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "com.shixianwen/voice_call"
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "startForegroundCall" -> {
                    val intent = Intent(this, VoiceCallService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    result.success(null)
                }
                "stopForegroundCall" -> {
                    stopService(Intent(this, VoiceCallService::class.java))
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }
}
