package com.lookseesee.app.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * Wraps Android's screen pinning (Lock Task Mode) API. This is the non-device-owner
 * flavor of kiosk mode: calling startLockTask() pins the current task so Home,
 * Recents and (on most stock/AOSP-derived Android 9+) the notification shade are
 * blocked. Exiting normally requires holding Back+Recents, which then asks for the
 * device's own screen lock credential if one is set - so pairing this with a device
 * PIN/pattern is recommended for a real toddler-proof seal.
 *
 * Phone calls are handled by the OS itself and always interrupt a pinned app; nothing
 * special is needed here for that.
 */
class LockTaskManager(private val activity: Activity) {

    fun pin() {
        if (isPinned()) return
        try {
            activity.startLockTask()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "startLockTask failed", e)
        }
    }

    fun unpin() {
        if (!isPinned()) return
        try {
            activity.stopLockTask()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "stopLockTask failed", e)
        }
    }

    fun isPinned(): Boolean {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return am?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    companion object {
        private const val TAG = "LockTaskManager"
    }
}
