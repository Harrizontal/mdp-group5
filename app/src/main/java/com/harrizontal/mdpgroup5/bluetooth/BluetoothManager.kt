package com.harrizontal.mdpgroup5.bluetooth

import android.bluetooth.BluetoothDevice
import java.io.Serializable
import java.util.ArrayList

class BluetoothManager private constructor() : Serializable {
    //    static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private val listBluetoothConnectionServices = ArrayList<BluetoothConnectionService>()
    private val devices = ArrayList<BluetoothDevice>()

//    fun addDevice(device: BluetoothDevice, handler: Handler) {
//
//        var deviceAdded: Boolean = false
//        devices.map {
//            if(it.address.equals(device.address)){
//                deviceAdded = true
//            }
//        }
//        if(!deviceAdded){
//            this.devices.add(device)
//            val bluetoothConnectionService = BluetoothConnectionService(handler, device)
//            listBluetoothConnectionServices.add(bluetoothConnectionService)
//        }
//        Log.d("BM","Added device to manager. "+"Total: "+devices.size)
//    }

//    fun sendMessage(message: String){
//        for (bluetoothService in listBluetoothConnectionServices) {
//            Log.d("BluetoothDeviceManager", "Writing data to bluetooth")
//            val bytes = message.toByteArray(Charset.defaultCharset())
//            bluetoothService.write(bytes)
//        }
//    }
//
//
//    fun connect(){
//        Log.d("BM","Connecting to ${listBluetoothConnectionServices.size} devices")
//        for (bluetoothService in listBluetoothConnectionServices) {
//            Log.d("BM","Connecting to ${bluetoothService.device.address} with ${bluetoothService.getState()}")
//            if (bluetoothService.getState() == BluetoothConstants.STATE_NONE) {
//                bluetoothService.connect()
//            }
//        }
//    }

//    fun reconnect() {
//        for (bluetoothService in listBluetoothConnectionServices) {
//            if (bluetoothService.getState() != BluetoothConstants.STATE_CONNECTED) {
//                bluetoothService.stop()
//                bluetoothService.connect()
//            }
//        }
//    }

    fun stopConnection() {
        for (bluetoothService in listBluetoothConnectionServices) {
            bluetoothService.stop()
        }
    }
//
//    fun sendMessageToAllDevices(data: ByteArray) {
//        for (bluetoothService in listBluetoothConnectionServices) {
//            Log.d("BluetoothDeviceManager", "Writing data to bluetooth")
//            bluetoothService.write(data)
//        }
//    }
//
//    fun sendMessageToDevice(deviceId: String, data: ByteArray) {
//        for (bluetoothService in listBluetoothConnectionServices) {
//            if (bluetoothService.device.address == deviceId)
//                bluetoothService.write(data)
//        }
//    }

    companion object {
        val instance = BluetoothManager()
    }


}