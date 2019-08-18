package com.harrizontal.mdpgroup5

import java.util.*

interface BluetoothConstants {

    enum class DeviceType {
        BLE, USB
    }

    companion object {

        val REQUEST_ENABLE_BT = 1

        // message types sent from the BluetoothChatService Handler
        val MESSAGE_STATE_CHANGE = 1
        val MESSAGE_READ = 2
        val MESSAGE_WRITE = 3
        val MESSAGE_SNACKBAR = 4

        // BluetoothConstants that indicate the current connection state
        val STATE_NONE = 0       // we're doing nothing, not connected.
        val STATE_LISTEN = 1     // listening for incoming connection, not connected
        val STATE_CONNECTING = 2 // now initiating an outgoing connection
        val STATE_CONNECTED = 3  // now connected to a remote device
        val STATE_ERROR = 4

        val myUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        val myPrivateUUID = UUID.fromString("00001101-0000-1000-5000-00805f9b34fb")


        val DEVICE_ADDRESS = "DEVICE_ADDRESS"
        val MESSAGE = "MESSAGE"
    }
}