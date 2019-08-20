package com.harrizontal.mdpgroup5.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.harrizontal.mdpgroup5.BluetoothConstants
import com.harrizontal.mdpgroup5.BluetoothConstants.Companion.MESSAGE_READ
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * Consist of AcceptedThread and ConnectedThread (for both act as a server and as a client)
 */
class BluetoothConnectionService internal constructor(mHandler: Handler){

    private var myHandler: Handler? = null
    private var device: BluetoothDevice? = null
    private var mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mState: Int = 0
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null

    private var retryCount = 1

    init {
        mState = BluetoothConstants.STATE_NONE
        myHandler = mHandler
    }

    @Synchronized
    fun start(){
        Log.d("BCS","Starting bluetoothconnectionservice")
        if(acceptThread == null){
            Log.d("BCS","Started AcceptThread")
            acceptThread = AcceptThread()
            acceptThread!!.start()
        }
    }

    @Synchronized
    internal fun connect(device: BluetoothDevice, handler: Handler) {
        Log.d("BCS","Connecting to: ${device.name} ${device.address}")

        // check if there is ongoing connection to bluetooth
        // if so, stop it
        if (connectedThread != null) {
            connectedThread!!.toStop()
            connectedThread!!.cancel()

            connectedThread = null
        }

        myHandler = handler
        setState(BluetoothConstants.STATE_CONNECTING)
        connectThread = ConnectThread(device)
        connectThread!!.start()
        retryCount++
    }

    @Synchronized
    internal fun stop() {
        cancelConnectThread()
        cancelConnectedThread()
        setState(BluetoothConstants.STATE_NONE)
    }

    @Synchronized
    private fun setState(state: Int) {
        Log.d("BCS","setState: "+ this.mState+" ->"+state)
        mState = state

        val data = Bundle()
        data.putString(BluetoothConstants.DEVICE_ADDRESS, device?.address)
        data.putInt(BluetoothConstants.MESSAGE, state)

        myHandler!!.obtainMessage(BluetoothConstants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget()
    }

    @Synchronized
    fun getState(): Int {
        return mState
    }

    @Synchronized
    fun write(data: ByteArray) {
        //Log.d("write() $data")
        if (connectedThread != null)
            connectedThread!!._write(data)
    }

    @Synchronized
    private fun connected(socket: BluetoothSocket, device: BluetoothDevice) {
        Log.d("connected to: %s", device.name)

        //cancelConnectThread()

        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }

        connectedThread = ConnectedThread(socket)
        connectedThread!!.start()

        setState(BluetoothConstants.STATE_CONNECTED)
    }

    /**
     * Connection Lost occurs when
     */
    private fun connectionLost() {
        Log.e("BCS","Connection Failed")
        if(acceptThread != null){
            acceptThread!!.cancel()
            acceptThread = null
        }

        this.start()
    }


    private fun cancelConnectThread() {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
    }

    private fun cancelConnectedThread() {
        if (connectedThread != null) {
            connectedThread!!.toStop()
            connectedThread!!.cancel()

            connectedThread = null
        }
    }


    private inner class AcceptThread() : Thread(){
        private val mmServerSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord("MDPGroup5",
                    BluetoothConstants.myUUID
                )
            } catch (e: IOException) {
                Log.e("AcceptThread","error: "+e)
            }

            mmServerSocket = tmp
            setState(BluetoothConstants.STATE_LISTEN)
        }

        override fun run() {
            Log.d(TAG, "run: AcceptThread Running.")

            var bluetoothSocket: BluetoothSocket? = null

            while(true){
                Log.d(TAG,"State: "+mState)
                try {
                    Log.d(TAG, "run: RFCOM server socket start.....")

                    bluetoothSocket = mmServerSocket!!.accept()

                    Log.d(TAG, "run: RFCOM server socket accepted connection.")
                } catch (e: IOException) {
                    Log.e(TAG, "AcceptThread: IOException: " + e.message)
                    break
                }

                if(bluetoothSocket != null){
                    connected(bluetoothSocket, bluetoothSocket.remoteDevice)
                    break;
                }
            }
            Log.i(TAG, "end: AcceptThread ")
        }

        fun cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.")
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.message)
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?

        init {
            var tmp: BluetoothSocket? = null
            try {
                val uuid = BluetoothConstants.myUUID
                tmp = mmDevice.createRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.e("ConnectThread","error: "+e)
            }

            mmSocket = tmp
        }

        override fun run() {
            try {
                mmSocket!!.connect()
            } catch (connectException: IOException) {
                // Unable to connect; close the socket and get out
                Log.e("ConnectThread","connectException error: "+connectException)
                try {
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                    Log.e("ConnectThread","closeException error: "+closeException)
                }

                //connectionLost()
                return
            }

            synchronized(this@BluetoothConnectionService) {
                connectThread = null
            }

            connected(mmSocket, mmDevice)
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        internal fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Close() socket failed", e)
            }

        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    inner class ConnectedThread internal constructor(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        private val mmBuffer: ByteArray = ByteArray(1024)
        private var isStop = false

        internal fun toStop() {
            isStop = true
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream

            } catch (e: IOException) {
                Log.e(TAG, "Temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream!!.read(mmBuffer)
                } catch (e: IOException) {
                    Log.e(TAG, "Input stream was disconnected", e)
                    setState(BluetoothConstants.STATE_NONE)
                    connectionLost()
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = myHandler!!.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer)
                readMsg.sendToTarget()
            }
        }


        /* Call this from the main activity to send data to the remote device */
        internal fun _write(bytes: ByteArray) {
            try {
                mmOutStream!!.write(bytes)
                myHandler!!.obtainMessage(BluetoothConstants.MESSAGE_WRITE, -1, -1, bytes).sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }

        }

        /* Call this from the main activity to shutdown the connection */

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }

        }
    }

    companion object {
        private val TAG = BluetoothConnectionService::class.java.simpleName
    }
}