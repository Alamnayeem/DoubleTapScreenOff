package com.doubletap.screenoff

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object LockFeedbackHelper {

    fun triggerFeedback(context: Context) {
        val prefs = context.getSharedPreferences("DoubleTapLockPrefs", Context.MODE_PRIVATE)
        val isVibrationEnabled = prefs.getBoolean("pref_vibration", true)
        val isSoundEnabled = prefs.getBoolean("pref_sound", true)
        val soundType = prefs.getString("pref_sound_type", "beep") ?: "beep"

        // 1. Play Vibration Feedback
        if (isVibrationEnabled) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(75)
                    }
                }
            }
        }

        // 2. Play Sound Feedback
        if (isSoundEnabled) {
            try {
                if (soundType == "beep") {
                    // Modern lightweight beep/tick tone generator
                    val toneGen = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 100)
                } else if (soundType == "notification") {
                    // Standard notification ringtone
                    val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
                    ringtone?.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
