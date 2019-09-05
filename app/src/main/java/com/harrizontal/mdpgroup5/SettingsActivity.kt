package com.harrizontal.mdpgroup5

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(btConnectionReceiver, IntentFilter("bluetoothConnectionStatus"))

    }

    private var btConnectionReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectionStatus = intent.getIntExtra("ConnectionStatus",99)
            Log.d("Settings","Received something :${connectionStatus}")
        }
    }
}