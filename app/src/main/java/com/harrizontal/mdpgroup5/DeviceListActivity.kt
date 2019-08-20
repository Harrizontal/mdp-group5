package com.harrizontal.mdpgroup5

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harrizontal.mdpgroup5.adapter.RecycleAdapter

class DeviceListActivity : Activity() {
    var EXTRA_DEVICE_ADDRESS = "device_address"

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var recycleAdapter: RecycleAdapter

    val pairedBluetooth: ArrayList<String> = ArrayList()
    val bluetooths: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(mReceiver, filter)

        bluetoothAdapter.startDiscovery()
        setResult(Activity.RESULT_CANCELED)

        // for bonded devices
        val pairedRecycleView = findViewById<RecyclerView>(R.id.list_paired)
        pairedRecycleView.layoutManager = LinearLayoutManager(this)
        val pairedDevices = bluetoothAdapter.getBondedDevices()
        val pairedRecycleAdapter = RecycleAdapter(pairedBluetooth,this)
        pairedRecycleView.adapter = pairedRecycleAdapter

        for (device in pairedDevices) {
            pairedBluetooth.add(device.address)
            pairedRecycleAdapter.notifyDataSetChanged()
        }



        // for Device found
        val recycleview = findViewById<RecyclerView>(R.id.list_bluetooth)
        recycleview.layoutManager = LinearLayoutManager(this)
        recycleAdapter = RecycleAdapter(bluetooths,this)
        recycleview.adapter = recycleAdapter

    }


    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery()
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver)
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
//                if (device.bondState != BluetoothDevice.BOND_BONDED) {
//                    mNewDevicesArrayAdapter.add(device.name + "\n" + device.address)
//                }
                // When discovery is finished, change the Activity title
                Log.d("MA","Name: ${device.name}, Address: ${device.address}, Device Class: ${device.bluetoothClass.majorDeviceClass}")

                bluetooths.add(device.address)
                recycleAdapter.notifyDataSetChanged()
                if(device.address.equals("18:3A:2D:D1:C0:71")){
                    Log.d("MA","Found device")
                    val intent = Intent()
                    intent.putExtra(EXTRA_DEVICE_ADDRESS,device.address)
                    setResult(RESULT_OK,intent)
                    //bluetoothManager.addDevice(device,mHandler)
                    //bluetoothService.connect(device,mHandler)
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                Log.d("MA","Discovery finished")
            }
        }
    }

}