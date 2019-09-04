package com.harrizontal.mdpgroup5.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class BService : IntentService("BluetoothIntentService"){

    private lateinit var mContext: Context

    override fun onHandleIntent(intent: Intent?) {
        mContext = applicationContext
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            // Restore interrupt status.
            Thread.currentThread().interrupt()
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BIS","Started")
        val connectionStatusIntent = Intent("btConnectionStatus")
        connectionStatusIntent.putExtra("ConnectionStatus", "connect")
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatusIntent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Toast.makeText(this, "service destroyed", Toast.LENGTH_LONG).show()
        super.onDestroy()
    }
}