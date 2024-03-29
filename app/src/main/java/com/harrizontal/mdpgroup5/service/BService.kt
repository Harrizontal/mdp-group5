package com.harrizontal.mdpgroup5.service

import android.app.IntentService
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.util.Log
import com.harrizontal.mdpgroup5.constants.BluetoothConstants
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.os.*
import com.harrizontal.mdpgroup5.constants.BluetoothConstants.Companion.STATE_RETRY_CONNECTING
import java.nio.charset.Charset
import java.util.*


class BService : Service(){


    private lateinit var mContext: Context
    private lateinit var mAdapter: BluetoothAdapter
    private var mState: Int = 0
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private var device: BluetoothDevice? = null

    private val myBinder = MyLocalBinder()

    private var retryTries = 3
    private var hardDisconnect = false

    override fun onBind(intent: Intent?): IBinder? {
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService() : BService {
            return this@BService
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BIS","Started with $startId")
        mContext = applicationContext
        mAdapter = BluetoothAdapter.getDefaultAdapter()

        device = intent!!.getExtras()!!.getParcelable<Parcelable>("device") as BluetoothDevice
        connectThread = ConnectThread(device!!)
        connectThread!!.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BIS","Service ended")
    }


    fun sendMessage(message: String){
        val bytes = message.toByteArray(Charset.defaultCharset())
        write(bytes)
    }

    @Synchronized
    private fun setState(state: Int) {
        Log.d("BCS","setState: "+ this.mState+" ->"+state)
        mState = state
        val intent = Intent("bluetoothConnectionStatus")
        intent.putExtra("ConnectionStatus", state)
        sendBroadcast(intent)
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
    fun stopAllConnection() {
        hardDisconnect = true
        setState(BluetoothConstants.STATE_NONE)
        cancelConnectThread()
        cancelConnectedThread()
        stopSelf() // stopAllConnection service
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

    @Synchronized
    private fun connected(socket: BluetoothSocket, device: BluetoothDevice) {
        Log.d("connected to: %s", device.name)

        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }



        connectedThread = ConnectedThread(socket)
        connectedThread!!.start()

        setState(BluetoothConstants.STATE_CONNECTED)
    }

    @Synchronized
    private fun reconnect(socket: BluetoothSocket){
        if (connectedThread != null) {
            connectedThread!!.toStop()
            connectedThread!!.cancel()

            connectedThread = null
        }

        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            var times: Int = 3
            override fun run() {
                Log.d("Timer","Connecting back in $times second")
                setState(STATE_RETRY_CONNECTING)
                if(times == 0){
                    timer.cancel()
                    connectThread = ConnectThread(socket.remoteDevice)
                    connectThread!!.start()
                }
                times--
            }

            override fun cancel(): Boolean {
                Log.d("Bservice","Timer ended")
                return super.cancel()
            }
        }, 0, 1000)



    }


    private inner class AcceptThread : Thread(){
        private val mmServerSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord("MDPGroup5",
                    BluetoothConstants.myUUID
                )
            } catch (e: IOException) {
                Log.e("AcceptThread","error: "+e)
            }

            mmServerSocket = tmp
            setState(BluetoothConstants.STATE_LISTEN)
        }

        override fun run() {
            Log.d("MA", "run: AcceptThread Running.")

            var bluetoothSocket: BluetoothSocket? = null

            while(true){
                Log.d("MA","State: "+mState)
                try {
                    Log.d("MA", "run: RFCOM server socket start.....")

                    bluetoothSocket = mmServerSocket!!.accept()

                    Log.d("MA", "run: RFCOM server socket accepted connection.")
                } catch (e: IOException) {
                    Log.e("MA", "AcceptThread: IOException: " + e.message)
                    break
                }

                if(bluetoothSocket != null){
                    connected(bluetoothSocket, bluetoothSocket.remoteDevice)
                    break;
                }
            }
            Log.i("MA", "end: AcceptThread ")
        }

        fun cancel() {
            Log.d("MA", "cancel: Canceling AcceptThread.")
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e("MA", "cancel: Close of AcceptThread ServerSocket failed. " + e.message)
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
                //tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid)
                tmp = mmDevice.createRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.e("ConnectThread","error: "+e)
            }

            mmSocket = tmp
        }

        override fun run() {
            try {
                setState(BluetoothConstants.STATE_CONNECTING)
                mmSocket!!.connect()
            } catch (connectException: IOException) {
                // Unable to connect; close the socket and get out
                setState(BluetoothConstants.STATE_ERROR)
                Log.e("ConnectThread","connectException error: "+connectException)

                if(retryTries > 0 && !hardDisconnect){
                    reconnect(mmSocket!!)
                }else{
                    // when user (from mobile app side) request to disconnect, it will stopAllConnection service
                    stopAllConnection()
                }

                stopSelf()
                try {
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                    Log.e("ConnectThread","closeException error: "+closeException)
                }

                return
            }

            synchronized(this@BService) {
                connectThread = null
            }

            connected(mmSocket, mmDevice)

            if(acceptThread != null){
                acceptThread!!.cancel()
                acceptThread = null
            }
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        internal fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e("ConnectThread", "Close() socket failed", e)
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
                Log.e("", "Temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            var numBytes: Int // bytes returned from read()
            var incomingMessage: String
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                try {
                    numBytes = mmInStream!!.read(mmBuffer)
                    incomingMessage = String(mmBuffer, 0, numBytes)
                } catch (e: IOException) {
                    Log.e("ConnectedThread", "Input stream was disconnected", e)
                    setState(BluetoothConstants.STATE_NONE)

                    if(retryTries > 0 && !hardDisconnect){
                        reconnect(mmSocket)
                    }else{
                        // when user (from mobile app side) request to disconnect, it will stopAllConnection service
                        stopAllConnection()
                    }
                    break
                }

                // broadcast message receive to all client
                val intent = Intent("bluetoothIncomingMessage")
                intent.putExtra("IncomingMessage",incomingMessage)
                //LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
                sendBroadcast(intent) // send to activities
            }
        }


        /* Call this from the main activity to send data to the remote device */
        internal fun _write(bytes: ByteArray) {
            try {
                mmOutStream!!.write(bytes)
                //myHandler!!.obtainMessage(BluetoothConstants.MESSAGE_WRITE, -1, -1, bytes).sendToTarget()
            } catch (e: IOException) {
                Log.e("ConnectedThread", "Exception during write", e)
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
                Log.e("" ,"close() of connect socket failed", e)
            }

        }
    }
}