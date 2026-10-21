package com.example.ggswidget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import android.widget.RemoteViews
import android.appwidget.AppWidgetManager
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.transition.Transition
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ImageLookup {
    companion object{
        fun setImage(context: Context, appWidgetId: Int, views: RemoteViews = buildWidgetViews(context, appWidgetId)) {
            val imageLinkID = Preferences.loadTitlePref(context, appWidgetId)
            val currentImageUrl = Preferences.loadCurrentImgPref(context, appWidgetId)
            if (currentImageUrl.isEmpty()) {
                Log.d("IMAGE_LOOKUP", "No Walltaker image cached yet for widget $appWidgetId.")
                GifWidgetAnimator.stop(appWidgetId)
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
                return
            }

            if (GifWidgetAnimator.isAnimatedGif(currentImageUrl)) {
                VideoWidgetAnimator.stop(appWidgetId)
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
                GifWidgetAnimator.start(context, appWidgetId, currentImageUrl)
                return
            }

            GifWidgetAnimator.stop(appWidgetId)

            if (VideoWidgetAnimator.isAnimatedVideo(currentImageUrl)) {
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
                VideoWidgetAnimator.start(context, appWidgetId, currentImageUrl)
                return
            }

            VideoWidgetAnimator.stop(appWidgetId)

            val cropToFill = Preferences.loadCheckedPref(context, appWidgetId) > 0
            val widgetSize = widgetPixelSize(context, appWidgetId)
            Log.d(
                "IMAGE_LOOKUP",
                "Loading image for widget $appWidgetId at ${widgetSize.width}x${widgetSize.height}px " +
                    "(cropToFill=$cropToFill)"
            )

            val awt: AppWidgetTarget = object : AppWidgetTarget(
                context.applicationContext,
                widgetSize.width,
                widgetSize.height,
                R.id.imageView2,
                views,
                appWidgetId
            ) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    super.onResourceReady(resource, transition)
                }
            }

            val request = Glide.with(context.applicationContext)
                .asBitmap()
                .override(widgetSize.width, widgetSize.height)
                .load(currentImageUrl)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("IMAGE_LOOKUP", "Unable to load current Walltaker image.")
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        val loadedImage = resource ?: return false
                        val prevImg = Preferences.loadPrevImagePref(context,appWidgetId)
                        val currentImg = bitmapToString(loadedImage)
                        if(prevImg != "" && currentImg != prevImg){
                            val sp = PreferenceManager.getDefaultSharedPreferences(context)
                            if(sp.getBoolean("notificationToggle",false)){
                                NotificationActivity().createNotificationChannel(context)
                                NotificationActivity().sendNotification(context,appWidgetId,Preferences.loadCurrentSenderPref(context,appWidgetId),imageLinkID)
                            }

                            Preferences.savePrevImagePref(context,appWidgetId,loadedImage)
                        }
                        return false
                    }
                })

            if (cropToFill) {
                request.centerCrop().into(awt)
            } else {
                request.fitCenter().into(awt)
            }

            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
        }

        private fun buildWidgetViews(context: Context, appWidgetId: Int): RemoteViews {
            val layout = if (Preferences.loadCheckedPref(context, appWidgetId) > 0) {
                R.layout.image_widget_cropped
            } else {
                R.layout.image_widget
            }
            val views = RemoteViews(context.packageName, layout)
            setClickable(context, views, appWidgetId)
            return views
        }

        private fun widgetPixelSize(context: Context, appWidgetId: Int): WidgetSize {
            val manager = AppWidgetManager.getInstance(context)
            val options = manager.getAppWidgetOptions(appWidgetId)
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE

            val widthDpKey = if (portrait) {
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            } else {
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            }
            val heightDpKey = if (portrait) {
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
            } else {
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
            }

            val providerInfo = manager.getAppWidgetInfo(appWidgetId)
            val widthDp = options.getInt(widthDpKey, providerInfo?.minWidth ?: DEFAULT_WIDGET_SIZE_DP)
                .coerceAtLeast(1)
            val heightDp = options.getInt(heightDpKey, providerInfo?.minHeight ?: DEFAULT_WIDGET_SIZE_DP)
                .coerceAtLeast(1)
            val density = context.resources.displayMetrics.density
            val widthPx = (widthDp * density).roundToInt().coerceAtLeast(1)
            val heightPx = (heightDp * density).roundToInt().coerceAtLeast(1)

            // Leave headroom below RemoteViews' device-dependent bitmap memory limit.
            val pixelCount = widthPx.toLong() * heightPx.toLong()
            if (pixelCount <= MAX_SAFE_BITMAP_PIXELS) {
                return WidgetSize(widthPx, heightPx)
            }

            val scale = sqrt(MAX_SAFE_BITMAP_PIXELS.toDouble() / pixelCount.toDouble())
            return WidgetSize(
                (widthPx * scale).roundToInt().coerceAtLeast(1),
                (heightPx * scale).roundToInt().coerceAtLeast(1)
            )
        }

        private const val DEFAULT_WIDGET_SIZE_DP = 300
        private const val MAX_SAFE_BITMAP_PIXELS = 4_000_000L

        private data class WidgetSize(val width: Int, val height: Int)
    }
}
