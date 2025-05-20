package com.github.fcopardo.zzz


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent

class SleepReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SleepReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) {
            Log.e(TAG, "Received null intent in onReceive!")
            return
        }

        Log.d(TAG, "Received intent with action: ${intent.action}")

        if (SleepSegmentEvent.hasEvents(intent)) {
            val sleepSegmentEvents: List<SleepSegmentEvent> = SleepSegmentEvent.extractEvents(intent)
            for (event in sleepSegmentEvents) {
                Log.d(TAG, "Sleep Segment: Start=${event.startTimeMillis}, End=${event.endTimeMillis}, " + "Status=${event.status}")
                Log.d(TAG, "  -- Segment duration: ${(event.endTimeMillis - event.startTimeMillis) / (1000 * 60)} minutes")
            }
        }
        else if (SleepClassifyEvent.hasEvents(intent)) {
            val sleepClassifyEvents: List<SleepClassifyEvent> = SleepClassifyEvent.extractEvents(intent)
            for (event in sleepClassifyEvents) {
                // SleepClassifyEvent *does* have 'confidence', 'motionPercentage', 'lightLux'
                Log.d(TAG, "Sleep Classify: Timestamp=${event.timestampMillis}, " +
                        "Confidence=${event.confidence}, Motion=${event.motion}, " +
                        "Light=${event.light}")
            }
        } else {
            Log.w(TAG, "Received intent without Sleep API events.")
        }
    }
}