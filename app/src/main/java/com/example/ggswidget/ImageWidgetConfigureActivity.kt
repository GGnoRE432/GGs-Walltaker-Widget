package com.example.ggswidget

import android.annotation.SuppressLint
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import com.example.ggswidget.databinding.ImageWidgetConfigureBinding

/**
 * The configuration screen for the [ImageWidget] AppWidget.
 */
class ImageWidgetConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var appWidgetLinkId: EditText
    private var onClickListener = View.OnClickListener {
        val context = this@ImageWidgetConfigureActivity

        if(findViewById<CheckBox>(R.id.checkBox).isChecked){
            saveCheckedPref(context, appWidgetId, 1)
        }else{
            saveCheckedPref(context, appWidgetId, 0)
        }

        // When the button is clicked, store the string locally
        val widgetLinkID = appWidgetLinkId.text.toString()
        saveTitlePref(context, appWidgetId, widgetLinkID)

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, appWidgetId)

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: ImageWidgetConfigureBinding

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestBatteryOptimizationExemption(context: Context) {
        if (!isIgnoringBatteryOptimizations(context)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        requestBatteryOptimizationExemption(this)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = ImageWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appWidgetLinkId = binding.appwidgetId
        binding.addButton.setOnClickListener(onClickListener)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        appWidgetLinkId.setText(loadTitlePref(this@ImageWidgetConfigureActivity, appWidgetId))
    }

}

private const val PREFS_NAME = "com.example.ggswidget.ImageWidget"
private const val PREF_PREFIX_KEY = "appwidget_"
private const val PREF_CHECKED_KEY = "pref_checked_key"

// Write the prefix to the SharedPreferences object for this widget
internal fun saveTitlePref(context: Context, appWidgetId: Int, text: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.putString(PREF_PREFIX_KEY + appWidgetId, text)
    prefs.apply()
}

// Read the prefix from the SharedPreferences object for this widget.
// If there is no preference saved, get the default from a resource
internal fun loadTitlePref(context: Context, appWidgetId: Int): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    val titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
    return titleValue ?: context.getString(R.string.appwidget_text)
}

// Same thing as with the title but for a value
internal fun saveCheckedPref(context: Context, appWidgetId: Int, value: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.putInt(PREF_CHECKED_KEY + appWidgetId, value)
    prefs.apply()
}

// Read the prefix from the SharedPreferences object for this widget.
// If there is no preference saved, get the default from a resource
internal fun loadCheckedPref(context: Context, appWidgetId: Int): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    val checkedValue = prefs.getInt(PREF_CHECKED_KEY + appWidgetId, appWidgetId)
    return checkedValue
}