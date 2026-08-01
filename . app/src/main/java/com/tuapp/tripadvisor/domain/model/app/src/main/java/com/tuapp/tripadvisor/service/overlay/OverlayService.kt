package com.tuapp.tripadvisor.service.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewTreeLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.tuapp.tripadvisor.MainActivity
import com.tuapp.tripadvisor.data.preferences.PreferencesRepository
import com.tuapp.tripadvisor.domain.evaluator.TripEvaluator
import com.tuapp.tripadvisor.domain.model.TripOffer
import com.tuapp.tripadvisor.domain.model.UserPreferences
import com.tuapp.tripadvisor.ui.overlay.SemaphoreWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_SHOW_OFFER = "com.tuapp.tripadvisor.SHOW_OFFER"
        const val ACTION_HIDE_OFFER = "com.tuapp.tripadvisor.HIDE_OFFER"

        const val EXTRA_DISTANCE_KM = "extra_distance_km"
        const val EXTRA_TIME_MINUTES = "extra_time_minutes"
        const val EXTRA_PRICE = "extra_price"

        private const val NOTIFICATION_CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var windowManager: WindowManager
    private lateinit var preferencesRepository: PreferencesRepository
    private var overlayView: ComposeView? = null
    private var cachedPreferences: UserPreferences? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        preferencesRepository = PreferencesRepository(applicationContext)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            preferencesRepository.userPreferencesFlow.first().let {
                cachedPreferences = it
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OFFER -> handleShowOffer(intent)
            ACTION_HIDE_OFFER -> hideOverlay()
        }
        return START_STICKY
    }

    private fun handleShowOffer(intent: Intent) {
        val distance = intent.getDoubleExtra(EXTRA_DISTANCE_KM, -1.0)
        val time = intent.getDoubleExtra(EXTRA_TIME_MINUTES, -1.0)
        val price = intent.getDoubleExtra(EXTRA_PRICE, -1.0)

        if (distance <= 0.0 || time <= 0.0 || price <= 0.0) return

        val offer = TripOffer(
            distanceKm = distance,
            estimatedTimeMinutes = time,
            offeredPrice = price
        )

        serviceScope.launch {
            val prefs = cachedPreferences ?: preferencesRepository.userPreferencesFlow.first()
            cachedPreferences = prefs

            val evaluation = TripEvaluator.evaluate(offer, prefs)
            showOverlay(evaluation)
        }
    }

    private fun showOverlay(evaluation: com.tuapp.tripadvisor.domain.model.TripEvaluation) {
        if (overlayView == null) {
            createOverlayView()
        }
        overlayView?.setContent {
            SemaphoreWidget(evaluation = evaluation)
        }
    }

    private fun createOverlayView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            getOverlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 150
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
        }

        overlayView = composeView
        windowManager.addView(composeView, params)
    }

    private fun hideOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    private fun getOverlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Semáforo de Viajes",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio activo que analiza tus ofertas de viaje"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Semáforo de Viajes activo")
            .setContentText("Analizando ofertas en segundo plano")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        hideOverlay()
        serviceScope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

