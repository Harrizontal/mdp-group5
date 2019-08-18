package com.harrizontal.mdpgroup5

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import android.annotation.SuppressLint
import android.os.Message
import com.harrizontal.mdpgroup5.bluetooth.BluetoothConnectionService
import com.harrizontal.mdpgroup5.bluetooth.BluetoothManager
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {


    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothService: BluetoothConnectionService
    private lateinit var mReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initDiscoverBluetooth()
    }


    private fun initDiscoverBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothManager = BluetoothManager.instance


        val pairedDevices = bluetoothAdapter.getBondedDevices()

        pairedDevices.map {
            Log.d("MA","Bonded devices name: "+it.name +  ", mac address: " + it.address)
        }


        // check permission needed for the phone's bluetooth to start detecting
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0
            )
        }


        bluetoothService = BluetoothConnectionService(mHandler).apply {
            start()
        }

        mReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                val action: String = intent.action!!
                when (action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        //Log.d("EPF","ACTION_FOUND")
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        Log.d("MA","Name: ${device.name}, Address: ${device.address}, Device Class: ${device.bluetoothClass.majorDeviceClass}")

                        if(device.address.equals("18:3A:2D:D1:C0:71")){
                            Log.d("MA","Found device")
                            //bluetoothManager.addDevice(device,mHandler)
                            bluetoothService.connect(device,mHandler)
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Log.d("MA","Discovery finished")
                        //bluetoothAdapter.startDiscovery() // enable scan again
                    }
                }
            }
        }



        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(mReceiver, filter)

        bluetoothAdapter.startDiscovery()


        button1.setOnClickListener {
            Log.d("MA","Start Discovering")
            bluetoothAdapter.startDiscovery()
        }

        button2.setOnClickListener {
            Log.d("MA","Sending data!")
            //bluetoothManager.sendMessage("Hello")
            val message = "r"
            val bytes = message.toByteArray(Charset.defaultCharset())
            bluetoothService.write(bytes)
        }

        button3.setOnClickListener {
            Log.d("MA","Disconnect")
            //bluetoothManager.stopConnection()
            bluetoothService.stop()
        }

        button4.setOnClickListener {
            Log.d("MA","Connect device")
            //bluetoothManager.connect()

        }

    }

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            Log.d("MA","Msg: ${msg.what} Data: ${msg.arg1}")
            when(msg.what){
                BluetoothConstants.MESSAGE_STATE_CHANGE -> {
                    when(msg.arg1){
                        BluetoothConstants.STATE_LISTEN -> {
                            textView.text = "Listening for incoming connection"
                        }
                        BluetoothConstants.STATE_CONNECTED -> {
                            textView.text = "Connected"
                        }
                        BluetoothConstants.STATE_CONNECTING -> {
                            textView.text = "Connecting"
                        }
                        BluetoothConstants.STATE_NONE -> {
                            textView.text = "Not connected"
                        }
                        BluetoothConstants.STATE_ERROR -> {
                            textView.text = "Something went wrong"
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


}
