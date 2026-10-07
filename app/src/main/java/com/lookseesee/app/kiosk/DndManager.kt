package com.lookseesee.app.kiosk

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Optional "silence notifications during play" setting. When enabled and granted,
 * this puts the phone into Priority DND with calls and media playback sound allowed
 * through, but pop-ups, notifications, and alert tones suppressed. Calls always get
 * through regardless of this setting or whether access is granted; this manager only
 * ever affects non-call notifications and never mutes the video/audio the viewer hears.
 */
class DndManager(private val context: Context) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    fun requestAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun beginSilence() {
        if (!hasAccess()) return
        notificationManager.notificationPolicy = NotificationManager.Policy(
            NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS or
                NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA,
            NotificationManager.Policy.PRIORITY_SENDERS_ANY,
            NotificationManager.Policy.PRIORITY_SENDERS_ANY,
        )
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun endSilence() {
        if (!hasAccess()) return
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }
}
