package com.harrizontal.mdpgroup5.bluetooth

import android.os.Handler
import android.os.Message
import android.util.Log
import com.harrizontal.mdpgroup5.BluetoothConstants


class BluetoothHandler: Handler(){
    override fun handleMessage(msg: Message?) {
        when(msg!!.what){
            BluetoothConstants.MESSAGE_STATE_CHANGE -> {
                when(msg.arg1){
                    BluetoothConstants.STATE_LISTEN -> {
                    }
                    BluetoothConstants.STATE_CONNECTED -> {
                    }
                    BluetoothConstants.STATE_CONNECTING -> {
                    }
                    BluetoothConstants.STATE_NONE -> {
                    }
                    BluetoothConstants.STATE_ERROR -> {
                    }
                }
            }
            BluetoothConstants.MESSAGE_READ -> {
                val readBuf = msg.obj as ByteArray
                // construct a string from the valid bytes in the buffer
                val readMessage = String(readBuf, 0, msg.arg1)
                Log.d("MA","Message recieved: $readMessage")

            }
            BluetoothConstants.MESSAGE_WRITE -> {
                val readBuf = msg.obj as ByteArray
                // construct a string from the valid bytes in the buffer
                val test = String(readBuf)
                Log.d("MA","Message sent: ${test}")
            }
            BluetoothConstants.MESSAGE_SNACKBAR -> {

            }
        }
    }
}