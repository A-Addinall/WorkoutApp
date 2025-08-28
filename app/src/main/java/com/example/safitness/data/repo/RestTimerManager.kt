package com.example.safitness.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.example.safitness.R
import com.example.safitness.data.repo.Repos
import com.example.safitness.ui.ExerciseDetailActivity
import com.example.safitness.ui.TimerBeeper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max

/**
 * Foreground service that mirrors the repository rest timer state:
 * - Shows an ongoing notification with remaining time.
 * - Plays countdown pips (5..1) and final buzz even in background.
 * - Provides Pause/Resume/Clear actions from the notification.
 *
 * Starts when a timer is running; stops when timer becomes null or reaches 0.
 */
class RestTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "rest_timer_channel"
        private const val CHANNEL_NAME = "Rest Timer"
        private const val NOTIF_ID = 9001

        // Actions
        private const val ACTION_START_OR_UPDATE = "com.example.safitness.action.START_OR_UPDATE"
        private const val ACTION_PAUSE = "com.example.safitness.action.PAUSE"
        private const val ACTION_RESUME = "com.example.safitness.action.RESUME"
        private const val ACTION_CLEAR = "com.example.safitness.action.CLEAR"

        // ✅ Minimal tweak: use startService() to avoid the 5s foreground timeout crash.
        fun startOrUpdate(ctx: Context) {
            val i = Intent(ctx, RestTimerService::class.java).setAction(ACTION_START_OR_UPDATE)
            ctx.startService(i)
        }

        fun pause(ctx: Context) {
            val i = Intent(ctx, RestTimerService::class.java).setAction(ACTION_PAUSE)
            ctx.startService(i)
        }

        fun resume(ctx: Context) {
            val i = Intent(ctx, RestTimerService::class.java).setAction(ACTION_RESUME)
            ctx.startService(i)
        }

        fun clear(ctx: Context) {
            val i = Intent(ctx, RestTimerService::class.java).setAction(ACTION_CLEAR)
            ctx.startService(i)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val beeper by lazy { TimerBeeper(this) }
    private val repo by lazy { Repos.workoutRepository(this) }

    // sound bookkeeping
    private var lastPippedSecond: Long = -1
    private var lastRemainingMs: Long? = null

    private var isInForeground = false
    private var collectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> repo.pauseRestTimer()
            ACTION_RESUME -> repo.resumeRestTimer()
            ACTION_CLEAR -> {
                resetSoundTracking()
                repo.clearRestTimer()
            }
            // START_OR_UPDATE or null: just ensure we’re collecting and in FG
        }

        ensureCollecting()
        return START_STICKY
    }

    private fun ensureCollecting() {
        // If already collecting, nothing to do.
        if (collectJob?.isActive == true) return

        collectJob = scope.launch {
            repo.restTimerState.collectLatest { st ->
                if (st == null || st.remainingMs <= 0) {
                    // Final buzz if we just crossed finish (only when not cleared manually)
                    val prevSec = lastRemainingMs?.div(1000L) ?: -1L
                    if (prevSec > 0) beeper.finalBuzz()
                    stopSelfSafely()
                    return@collectLatest
                }

                // ensure foreground once we have state
                val notif = buildNotification(st.remainingMs, st.isRunning)
                if (!isInForeground) {
                    startForeground(NOTIF_ID, notif)
                    isInForeground = true
                } else {
                    val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    mgr.notify(NOTIF_ID, notif)
                }

                // SOUND in background
                handleBackgroundBeeps(st.remainingMs)
            }
        }
    }

    private fun buildNotification(remainingMs: Long, isRunning: Boolean): Notification {
        val remainText = formatMs(remainingMs)
        val title = getString(R.string.app_name)
        val content = if (isRunning) getString(R.string.rest_timer_running, remainText)
        else getString(R.string.rest_timer_paused, remainText)

        // Tap opens ExerciseDetailActivity (back stack preserved)
        val contentIntent = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(Intent(this, ExerciseDetailActivity::class.java))
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag())

        // Actions
        val pauseAction = NotificationCompat.Action(
            0, getString(R.string.pause),
            PendingIntent.getService(
                this, 1,
                Intent(this, RestTimerService::class.java).setAction(ACTION_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
            )
        )
        val resumeAction = NotificationCompat.Action(
            0, getString(R.string.resume),
            PendingIntent.getService(
                this, 2,
                Intent(this, RestTimerService::class.java).setAction(ACTION_RESUME),
                PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
            )
        )
        val clearAction = NotificationCompat.Action(
            0, getString(R.string.clear),
            PendingIntent.getService(
                this, 3,
                Intent(this, RestTimerService::class.java).setAction(ACTION_CLEAR),
                PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
            )
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // swap to a timer icon if you have one
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(isRunning)
            .setSilent(true) // we play our own beeps
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (isRunning) builder.addAction(pauseAction) else builder.addAction(resumeAction)
        builder.addAction(clearAction)

        return builder.build()
    }

    private fun handleBackgroundBeeps(currentRemainingMs: Long) {
        val prev = lastRemainingMs
        lastRemainingMs = currentRemainingMs

        val sec = currentRemainingMs / 1000L
        if (sec in 1..5 && sec != lastPippedSecond) {
            beeper.countdownPip()
            lastPippedSecond = sec
        }

        val prevSec = prev?.div(1000L) ?: -1L
        if (prevSec > 0 && sec <= 0) {
            beeper.finalBuzz()
            resetSoundTracking()
        }
    }

    private fun resetSoundTracking() {
        lastPippedSecond = -1
        lastRemainingMs = null
    }

    private fun stopSelfSafely() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.cancel(NOTIF_ID)
        if (isInForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isInForeground = false
        }
        stopSelf()
        collectJob?.cancel()
        collectJob = null
        beeper.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        collectJob?.cancel()
        beeper.release()
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows and controls the rest timer"
            setSound(null, null) // we use ToneGenerator instead
        }
        mgr.createNotificationChannel(ch)
    }

    private fun formatMs(ms: Long): String {
        val total = max(0L, ms / 1000L)
        val m = total / 60
        val s = total % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun mutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_MUTABLE else 0
}
