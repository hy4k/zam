package com.example.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.DisconnectCause
import androidx.core.app.NotificationCompat
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import com.example.MainActivity
import com.example.R
import com.example.data.model.FamilyMemberId
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android Core-Telecom Framework Integration:
 * - Registers Holy Moly ! as a self-managed VoIP calling app with video + audio + Bluetooth routing.
 * - Supports 1-on-1 & 3-way group video and voice calls, clean hangup teardown, and lock-screen
 *   full-screen incoming call notifications.
 */
class FamilyTelecomManager(private val context: Context) {

    private val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w("FamilyTelecomManager", "Telecom coroutine error", throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineErrorHandler)
    private val callsManager = CallsManager(context.applicationContext)
    private var isRegistered = false

    private var activeCallJob: Job? = null
    private var activeDisconnectTrigger: CompletableDeferred<Unit>? = null

    private val _availableEndpoints = MutableStateFlow<List<CallEndpointCompat>>(emptyList())
    val availableEndpoints: StateFlow<List<CallEndpointCompat>> = _availableEndpoints.asStateFlow()

    private val _currentEndpoint = MutableStateFlow<CallEndpointCompat?>(null)
    val currentEndpoint: StateFlow<CallEndpointCompat?> = _currentEndpoint.asStateFlow()

    fun registerTelecomCapabilities() {
        if (isRegistered) return
        runCatching {
            callsManager.registerAppWithTelecom(
                capabilities = CallsManager.CAPABILITY_SUPPORTS_VIDEO_CALLING
            )
            isRegistered = true
        }
    }

    fun startManagedFamilyCall(
        callerOrTarget: FamilyMemberId,
        isIncoming: Boolean,
        isVideo: Boolean = true,
        onAnswered: suspend () -> Unit,
        onDisconnected: suspend () -> Unit
    ) {
        endManagedCall()
        registerTelecomCapabilities()
        val direction = if (isIncoming) {
            CallAttributesCompat.DIRECTION_INCOMING
        } else {
            CallAttributesCompat.DIRECTION_OUTGOING
        }
        val callType = if (isVideo) {
            CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        } else {
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL
        }

        val attributes = CallAttributesCompat(
            displayName = "${callerOrTarget.displayName} (${callerOrTarget.title})",
            address = Uri.parse("kinnest://family/${callerOrTarget.name.lowercase()}"),
            direction = direction,
            callType = callType,
            callCapabilities = CallAttributesCompat.SUPPORTS_SET_INACTIVE or
                CallAttributesCompat.SUPPORTS_STREAM
        )

        val disconnectSignal = CompletableDeferred<Unit>()
        activeDisconnectTrigger = disconnectSignal

        activeCallJob = scope.launch {
            runCatching {
                callsManager.addCall(
                    callAttributes = attributes,
                    onAnswer = { _ -> onAnswered() },
                    onDisconnect = { _ ->
                        disconnectSignal.complete(Unit)
                        onDisconnected()
                    },
                    onSetActive = { },
                    onSetInactive = { }
                ) {
                    val endpointsJob = launch {
                        availableEndpoints.collect { endpoints ->
                            _availableEndpoints.value = endpoints
                        }
                    }
                    val currentJob = launch {
                        currentCallEndpoint.collect { endpoint ->
                            _currentEndpoint.value = endpoint
                        }
                    }
                    if (!isIncoming) {
                        launch { runCatching { setActive() } }
                    }
                    // Keep the managed Telecom session alive until endManagedCall() or system disconnect
                    launch {
                        disconnectSignal.await()
                        endpointsJob.cancel()
                        currentJob.cancel()
                        runCatching { disconnect(DisconnectCause(DisconnectCause.LOCAL)) }
                    }
                }
            }
        }
    }

    fun endManagedCall() {
        activeDisconnectTrigger?.complete(Unit)
        activeDisconnectTrigger = null
        activeCallJob?.cancel()
        activeCallJob = null
    }

    /**
     * Posts a high-priority lock-screen incoming call notification (matching video vs voice calls).
     */
    fun postLockScreenIncomingCallNotification(
        caller: FamilyMemberId,
        isThreeWay: Boolean,
        videoEnabled: Boolean = true,
        quietHoursActive: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = if (quietHoursActive) {
                NotificationManager.IMPORTANCE_DEFAULT
            } else {
                NotificationManager.IMPORTANCE_HIGH
            }
            val channel = NotificationChannel(
                CHANNEL_INCOMING_CALL,
                context.getString(R.string.call_notification_channel_name),
                importance
            ).apply {
                description = "Lock-screen alerts for Mithun, Mafitha, and Zayn Adam calls"
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ACTION, ACTION_INCOMING_CALL)
            putExtra(EXTRA_CALLER_ID, caller.name)
            putExtra(EXTRA_THREE_WAY, isThreeWay)
            putExtra(EXTRA_VIDEO_ENABLED, videoEnabled)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            101,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callKind = if (videoEnabled) "Video Call" else "Voice Call"
        val title = if (isThreeWay) {
            "${caller.displayName} started a 3-Way $callKind"
        } else {
            "${caller.displayName} (${caller.title}) • Incoming $callKind"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_INCOMING_CALL)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(
                if (videoEnabled) "Tap Accept to jump into the family video room"
                else "Tap Accept to join the family voice call"
            )
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(
                if (quietHoursActive) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_MAX
            )
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        runCatching {
            manager.notify(NOTIFICATION_ID_INCOMING, notification)
        }
    }

    fun cancelIncomingCallNotification() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID_INCOMING)
    }

    companion object {
        const val CHANNEL_INCOMING_CALL = "kinnest_family_calls"
        const val NOTIFICATION_ID_INCOMING = 4001
        const val EXTRA_ACTION = "kinnest_action"
        const val EXTRA_CALLER_ID = "kinnest_caller_id"
        const val EXTRA_THREE_WAY = "kinnest_three_way"
        const val EXTRA_VIDEO_ENABLED = "kinnest_video_enabled"
        const val ACTION_INCOMING_CALL = "action_incoming_call"
        const val ACTION_QUICK_CALL = "action_quick_call"
        const val ACTION_QUICK_HEARTBEAT = "action_quick_heartbeat"
    }
}
