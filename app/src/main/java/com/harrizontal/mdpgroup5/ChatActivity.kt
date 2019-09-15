package com.harrizontal.mdpgroup5

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import com.harrizontal.mdpgroup5.service.BService

/**
 *
 * Not in requirements. For debugging bidirectional communication between raspberry pi
 */
class ChatActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

//        LocalBroadcastManager.getInstance(this)
//            .registerReceiver(btConnectionReceiver, IntentFilter("bluetoothConnectionStatus"))

    }

    override fun onDestroy() {
        Log.d("SettingsActivity","onDestory")
        super.onDestroy()

    }

    override fun onResume() {
        Log.d("SettingsActivity","onResume")
        super.onResume()
    }

    private var btConnectionReceiver: BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val connectionStatus = intent.getIntExtra("ConnectionStatus",99)
            Log.d("Settings","Received something :${connectionStatus}")
            val testTextView = findViewById<TextView>(R.id.textview_Hello)
            testTextView.text = connectionStatus.toString()
        }
    }
}