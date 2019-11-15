package com.harrizontal.mdpgroup5

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import kotlinx.android.synthetic.main.activity_disconnect_activity.*

class DisconnectBluetoothActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disconnect_activity)

        val buttonDisconnect = findViewById<Button>(R.id.button_disconnect)

        buttonDisconnect.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }

        button_cancel.setOnClickListener {
            finish()
        }
    }

}