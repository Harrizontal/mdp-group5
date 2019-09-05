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
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.harrizontal.mdpgroup5.constants.BluetoothConstants
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.os.*
import android.os.Messenger
import com.harrizontal.mdpgroup5.constants.MDPConstants
import java.nio.charset.Charset


class BService : Service(){


    private lateinit var mContext: Context
    private lateinit var mAdapter: BluetoothAdapter
    private var mState: Int = 0
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private var device: BluetoothDevice? = null

    private val myBinder = MyLocalBinder()


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
        Log.d("", "Service Handle: startClientThread")
        connectThread = ConnectThread(device!!,startId)
        connectThread!!.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BIS","Service ended")
        val intent = Intent("bluetoothConnectionStatus")
        intent.putExtra("ConnectionStatus", BluetoothConstants.STATE_NONE)
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
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
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
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
    fun stop() {
        cancelConnectThread()
        cancelConnectedThread()
        setState(BluetoothConstants.STATE_NONE)
        stopSelf() // stop service
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

        //cancelConnectThread()

        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }

        connectedThread = ConnectedThread(socket)
        connectedThread!!.start()

        setState(BluetoothConstants.STATE_CONNECTED)
        val intent = Intent("bluetoothConnectionStatus")
        intent.putExtra("ConnectionStatus", BluetoothConstants.STATE_CONNECTED)
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
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
            Log.d("", "run: AcceptThread Running.")

            var bluetoothSocket: BluetoothSocket? = null

            while(true){
                Log.d("","State: "+mState)
                try {
                    Log.d("", "run: RFCOM server socket start.....")

                    bluetoothSocket = mmServerSocket!!.accept()

                    Log.d("", "run: RFCOM server socket accepted connection.")
                } catch (e: IOException) {
                    Log.e("", "AcceptThread: IOException: " + e.message)
                    break
                }

                if(bluetoothSocket != null){
                    connected(bluetoothSocket, bluetoothSocket.remoteDevice)
                    break;
                }
            }
            Log.i("", "end: AcceptThread ")
        }

        fun cancel() {
            Log.d("", "cancel: Canceling AcceptThread.")
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e("", "cancel: Close of AcceptThread ServerSocket failed. " + e.message)
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice, private val startId: Int) : Thread() {
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
                setState(BluetoothConstants.STATE_ERROR)
                Log.e("ConnectThread","connectException error: "+connectException)
                stopSelf(startId)
                try {
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                    Log.e("ConnectThread","closeException error: "+closeException)
                }

                //connectionLost()
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
                Log.e("", "Close() socket failed", e)
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
                    Log.e("", "Input stream was disconnected", e)
                    setState(BluetoothConstants.STATE_NONE)
                    stopSelf()

                    break
                }

                // broadcast message receive to all client
                val intent = Intent("bluetoothIncomingMessage")
                intent.putExtra("IncomingMessage",incomingMessage)
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent)
            }
        }


        /* Call this from the main activity to send data to the remote device */
        internal fun _write(bytes: ByteArray) {
            try {
                mmOutStream!!.write(bytes)
                //myHandler!!.obtainMessage(BluetoothConstants.MESSAGE_WRITE, -1, -1, bytes).sendToTarget()
            } catch (e: IOException) {
                Log.e("", "Exception during write", e)
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