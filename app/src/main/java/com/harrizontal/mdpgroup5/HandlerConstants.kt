package com.harrizontal.mdpgroup5

import java.util.*

interface HandlerConstants {

    companion object {

        // message types sent from the BluetoothChatService Handler
        val MESSAGE_STATE_CHANGE = 1
        val MESSAGE_READ = 2
        val MESSAGE_WRITE = 3
        val MESSAGE_SNACKBAR = 4
    }
}