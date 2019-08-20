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
import android.app.Activity
import android.app.PendingIntent.getActivity
import android.os.Message
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.harrizontal.mdpgroup5.bluetooth.BluetoothConnectionService
import com.harrizontal.mdpgroup5.bluetooth.BluetoothManager
import java.nio.charset.Charset
import android.widget.GridView
import com.harrizontal.mdpgroup5.adapter.GridAdapter


class MainActivity : AppCompatActivity() {


    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothService: BluetoothConnectionService
    private lateinit var mReceiver: BroadcastReceiver

    private val REQUEST_BLUETOOTH_CONNECTION = 1

    private val books: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val gridView = findViewById<GridView>(R.id.grid_maze)
//        val booksAdapter = GridAdapter(this, books,20,15)
//        gridView.adapter = booksAdapter

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
                            //bluetoothService.connect(device,mHandler)
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Log.d("MA","Discovery finished")
                        //bluetoothAdapter.startDiscovery() // enable scan again
                    }
                }
            }
        }



//        val filter = IntentFilter()
//        filter.addAction(BluetoothDevice.ACTION_FOUND)
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//        registerReceiver(mReceiver, filter)

        //bluetoothAdapter.startDiscovery()


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

    override fun onDestroy() {
        Log.d("MA","onDestory")
        super.onDestroy()
        if(bluetoothService != null){
            bluetoothService.stop()
        }
    }

    override fun onResume() {
        Log.d("MA","onResume")
        super.onResume()
        if(bluetoothService != null){
            if(bluetoothService.getState() == BluetoothConstants.STATE_NONE){
                bluetoothService.start()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.secure_connect_scan -> {
                val serverIntent = Intent(this, DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_BLUETOOTH_CONNECTION)
                true
            }
            else -> super.onOptionsItemSelected(item)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode){
            REQUEST_BLUETOOTH_CONNECTION -> {
                if(resultCode == Activity.RESULT_OK){
                    val address = data?.extras?.getString("device_address")
                    Log.d("MA","requestCode: $requestCode, resultCode: $resultCode, data: $data, device_address: $address")
                    bluetoothService.connect(bluetoothAdapter.getRemoteDevice(address),mHandler)
                }
            }
        }
    }


}
