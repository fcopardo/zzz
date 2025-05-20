package com.github.fcopardo.zzz

// MainActivity.kt

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var sleepPendingIntent: PendingIntent

    // Launcher for requesting the ACTIVITY_RECOGNITION permission
    private val activityRecognitionPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "ACTIVITY_RECOGNITION permission granted.")
                subscribeToSleepUpdates()
            } else {
                Log.w(TAG, "ACTIVITY_RECOGNITION permission denied.")
                Toast.makeText(this, "Sleep API requires Activity Recognition permission.", Toast.LENGTH_LONG).show()
                // Consider showing a UI explanation to the user here.
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val subscribeButton: Button = findViewById(R.id.subscribe_button)
        val unsubscribeButton: Button = findViewById(R.id.unsubscribe_button)

        // --- Create the PendingIntent for your BroadcastReceiver ---
        val intent = Intent(this, SleepReceiver::class.java)
        var flags = PendingIntent.FLAG_CANCEL_CURRENT

        // CRUCIAL for Android 12+ (API 31+): Declare mutability
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 is API 31
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        sleepPendingIntent = PendingIntent.getBroadcast(this, 0, intent, flags)
        // ----------------------------------------------------------

        subscribeButton.setOnClickListener {
            checkAndRequestActivityRecognitionPermission()
        }

        unsubscribeButton.setOnClickListener {
            unsubscribeFromSleepUpdates()
        }
    }

    private fun checkAndRequestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // ACTIVITY_RECOGNITION introduced in API 29
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "ACTIVITY_RECOGNITION permission already granted.")
                subscribeToSleepUpdates()
            } else {
                Log.d(TAG, "Requesting ACTIVITY_RECOGNITION permission.")
                activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        } else {
            // For devices below Android 10, ACTIVITY_RECOGNITION is generally not required
            // or handled differently if targetSdkVersion < 29. Our minSdk is 29, so this block
            // will primarily be for devices running API 29-33 if we were targeting lower than 29.
            // With minSdk 29, permission check is always relevant.
            Log.d(TAG, "ACTIVITY_RECOGNITION permission check handled for API < 29 (though minSdk is 29).")
            subscribeToSleepUpdates()
        }
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    private fun subscribeToSleepUpdates() {
        Log.d(TAG, "Attempting to subscribe to sleep updates...")
        ActivityRecognition.getClient(this)
            .requestSleepSegmentUpdates(
                sleepPendingIntent,
                SleepSegmentRequest.getDefaultSleepSegmentRequest()
            )
            .addOnSuccessListener {
                Log.d(TAG, "Successfully subscribed to sleep data. Check Logcat for updates.")
                Toast.makeText(this, "Subscribed to sleep data!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Exception when subscribing to sleep data: $exception")
                Toast.makeText(this, "Failed to subscribe: ${exception.message}", Toast.LENGTH_LONG).show()
            }

        // You might also want to subscribe to SleepClassifyEvents for more frequent (but less definitive) updates:
        // ActivityRecognition.getClient(this)
        //    .requestSleepClassifyUpdates(
        //        sleepPendingIntent,
        //        SleepClassifyRequest.getDefaultSleepClassifyRequest()
        //    )
        //    .addOnSuccessListener { Log.d(TAG, "Successfully subscribed to sleep classify data.") }
        //    .addOnFailureListener { exception -> Log.e(TAG, "Exception subscribing to classify data: $exception") }
    }

    private fun unsubscribeFromSleepUpdates() {
        Log.d(TAG, "Attempting to unsubscribe from sleep updates...")
        ActivityRecognition.getClient(this)
            .removeSleepSegmentUpdates(sleepPendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully unsubscribed from sleep data.")
                Toast.makeText(this, "Unsubscribed from sleep data!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Exception when unsubscribing from sleep data: $exception")
                Toast.makeText(this, "Failed to unsubscribe: ${exception.message}", Toast.LENGTH_LONG).show()
            }

        // Also unsubscribe from SleepClassifyEvents if you subscribed to them
        // ActivityRecognition.getClient(this)
        //    .removeSleepClassifyUpdates(sleepPendingIntent)
        //    .addOnSuccessListener { Log.d(TAG, "Successfully unsubscribed from sleep classify data.") }
        //    .addOnFailureListener { exception -> Log.e(TAG, "Exception unsubscribing classify data: $exception") }
    }

    override fun onDestroy() {
        super.onDestroy()
        // It's good practice to unsubscribe when your component is destroyed,
        // especially if you don't intend to receive updates in the background.
        // If your app is designed to run in the background (e.g., foreground service),
        // you might manage subscriptions differently.
        unsubscribeFromSleepUpdates()
    }
}