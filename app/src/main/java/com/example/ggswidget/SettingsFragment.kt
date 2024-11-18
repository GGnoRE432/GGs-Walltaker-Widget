package com.example.ggswidget

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onStop() {
        super.onStop()
        val intent = Intent(context, ImageWidget::class.java).apply {
            action = "com.example.app.ACTION_UPDATE_WIDGET"
        }

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        pendingIntent.send()
    }
}