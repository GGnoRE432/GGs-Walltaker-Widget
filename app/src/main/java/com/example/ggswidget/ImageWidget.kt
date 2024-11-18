package com.example.ggswidget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.transition.Transition
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.Executors

class ImageWidget : AppWidgetProvider() {

    val ACTION_UPDATE_WIDGET = "com.example.app.ACTION_UPDATE_WIDGET"
    val WIDGET_CLICK = "com.example.app.WIDGET_CLICK"
    val UPDATE_TIME = "com.example.app.UPDATE_TIME"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created

        setAlarm(context)

    }

    @SuppressLint("ScheduleExactAlarm")
    private fun setAlarm(context: Context) {
        // Set up the intent to refresh the widget
        val intent = Intent(context, ImageWidget::class.java).apply {
            action = "com.example.app.ACTION_UPDATE_WIDGET"
        }

        // Create a PendingIntent wrapping the Intent
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val updateInterval = sp.getString("updateInterval","10")
        var updateTime = 10
        Log.d("UPDATE INTERVAL", updateInterval.toString())
        try {
            if (updateInterval != null) {
                updateTime = updateInterval.toInt()
                Log.d("TO INT", updateTime.toString())
            }
        } catch (e: NumberFormatException) {
            updateTime = 10
            Log.d("CATCH", updateTime.toString())
        }
        updateTime *= 1000
        Log.d("MULTIPLY", updateTime.toString())
        if(updateTime < 10000){
            updateTime = 10000
        }
        Log.d("IS VALID", updateTime.toLong().toString())

        val aManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        aManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + updateTime.toLong(), pendingIntent)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        val aManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Set up the intent to refresh the widget
        val intent = Intent(context, ImageWidget::class.java).apply {
            action = "com.example.app.ACTION_UPDATE_WIDGET"
        }

        // Create a PendingIntent wrapping the Intent
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        aManager.cancel(pendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_UPDATE_WIDGET || intent.action == UPDATE_TIME) {
            setAlarm(context)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ImageWidget::class.java))
            onUpdate(context, appWidgetManager, appWidgetIds)
        }

        if(intent.action == WIDGET_CLICK){
            try {
                val linkID = intent.getStringExtra("KEY_ID").toString()
                val webIntent = Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://walltaker.joi.how/links/$linkID"))
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            } catch (e: RuntimeException) {
                // The url is invalid, maybe missing http://
                e.printStackTrace()
            }
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val imageLinkID = loadTitlePref(context, appWidgetId)
    val cropImage = loadCheckedPref(context, appWidgetId)
    var views = RemoteViews(context.packageName, R.layout.image_widget)
    Log.d("CROP?", cropImage.toString())

    if(cropImage > 0){
        views = RemoteViews(context.packageName, R.layout.image_widget_cropped)
    }

    val awt: AppWidgetTarget = object : AppWidgetTarget(context.applicationContext, R.id.imageView2, views, appWidgetId) {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            super.onResourceReady(resource, transition)
        }
    }

    Glide.with(context.applicationContext).asBitmap().override(2000,2000).fitCenter().load(getImageFromURL(imageLinkID)).into(awt)

    setClickable(context, views, appWidgetId)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

fun setClickable(context: Context, views: RemoteViews, appWidgetId: Int) {
    val imageLinkID = loadTitlePref(context, appWidgetId)
    // Set up the intent to press the widget
    val intent = Intent(context, ImageWidget::class.java).apply {
        action = "com.example.app.WIDGET_CLICK"
    }
    intent.putExtra("KEY_ID", imageLinkID)
    Log.d("SET EXTRA", intent.getStringExtra("KEY_ID").toString())

    // Create a PendingIntent wrapping the Intent
    val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    views.setOnClickPendingIntent(R.id.widgetFrameLayout, pendingIntent)
}

fun getJsonDataFromUrl(url: String): String {

    val connection = URL(url).openConnection()
    connection.addRequestProperty("User-Agent", "GGWidget/")
    Log.d("HEADER", connection.getRequestProperty("User-Agent"))
    val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
    val jsonData = StringBuilder()

    var line: String?
    while (reader.readLine().also { line = it } != null) {
        jsonData.append(line)
    }
    reader.close()

    return jsonData.toString()
}

fun getImageFromURL(id: String): String{

    val executor = Executors.newSingleThreadExecutor();

    val handler = Handler(Looper.getMainLooper())

    var imageURL = ""

    executor.execute {

        val walltakerURL = "https://walltaker.joi.how/links/$id.json"

        try {
            val jsonData = getJsonDataFromUrl(walltakerURL) // Replace with your JSON URL
            val jsonObject = JSONObject(jsonData)
            imageURL = jsonObject.getString("post_url")
        } catch (e: Exception) {
            Log.d("UNAVAILABLE", "Service is unavailable!");
        }

    }
    Thread.sleep(1000)
    return imageURL
}
