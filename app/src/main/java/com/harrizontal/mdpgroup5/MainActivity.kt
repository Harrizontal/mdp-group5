package com.harrizontal.mdpgroup5

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import android.app.AlertDialog
import android.content.*
import android.os.IBinder
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harrizontal.mdpgroup5.adapter.*
import com.harrizontal.mdpgroup5.bluetooth.BluetoothConnectionService
import com.harrizontal.mdpgroup5.bluetooth.BluetoothManager
import com.harrizontal.mdpgroup5.constants.ActivityConstants
import com.harrizontal.mdpgroup5.constants.BluetoothConstants
import com.harrizontal.mdpgroup5.constants.MDPConstants
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_FUNCTION_1
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_FUNCTION_2
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_MAP_UPDATE
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_FUNCTION_1
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_FUNCTION_2
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_MAP_UPDATE
import com.harrizontal.mdpgroup5.helper.Utils
import com.harrizontal.mdpgroup5.service.BService

class MainActivity : AppCompatActivity() {


    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var mMapDescriptor: ArrayList<Char> = ArrayList()

    private lateinit var mazeAdapter: MazeAdapter

    var myService: BService? = null
    var isBound = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.setSubtitle("Not connected")

        registerReceiver(bluetoothConnectionReceiver, IntentFilter("bluetoothConnectionStatus"))
        registerReceiver(bluetoothIncomingMessage,IntentFilter("bluetoothIncomingMessage"))

        //mMapDescriptor = Utils().getMapDescriptor(MDPConstants.DEFAULT_MAP_DESCRIPTOR_STRING)

        Log.d("MA","Size of Descriptor: ${mMapDescriptor!!.size}")

        // yAxis
        val yAxisGridView = findViewById<RecyclerView>(R.id.yAxis)
        val ylayoutManager = GridLayoutManager(this,1)
        val yAxisAdapter = MazeAxisAdapter(this,MDPConstants.NUM_ROWS,1)
        yAxisGridView.layoutManager = ylayoutManager
        yAxisGridView.adapter = yAxisAdapter

        // xAxis
        val xAxisGridView = findViewById<RecyclerView>(R.id.xAxis)
        val xlayoutManager = GridLayoutManager(this,MDPConstants.NUM_COLUMNS)
        val xAxisAdapter = MazeAxisAdapter(this,MDPConstants.NUM_COLUMNS,0)
        xAxisGridView.layoutManager = xlayoutManager
        xAxisGridView.adapter = xAxisAdapter


        // maze
        val recycleViewMaze = findViewById<RecyclerView>(R.id.recyclerview_maze)
        val gridLayoutManager = GridLayoutManager(this,MDPConstants.NUM_COLUMNS)
        mazeAdapter = MazeAdapter(this,MDPConstants.NUM_ROWS,MDPConstants.NUM_COLUMNS,mMapDescriptor)
        recycleViewMaze.layoutManager = gridLayoutManager
        recycleViewMaze.adapter = mazeAdapter



        button_test.setOnClickListener {
            Utils().convertCoordinatesToGridId(14,0)
        }

        button_turnleft.setOnClickListener {
            sendMessageToBluetooth("mov:left,1")
        }

        button_turnRight.setOnClickListener {
            sendMessageToBluetooth("mov:right,1")
        }

        button_up.setOnClickListener {
            sendMessageToBluetooth("mov:forward,1")
        }

        button_exploration.setOnClickListener {
            sendMessageToBluetooth("alg:explore")
        }

        button_fastest_path.setOnClickListener {
            sendMessageToBluetooth("alg:fast")
        }

        initializeUpdateMap()

        initDiscoverBluetooth()
    }

    /**
     * Update Map button will only apply if Update Map switch at Settings page is checked
     */
    private fun initializeUpdateMap(){
        val sharedPref: SharedPreferences = getSharedPreferences(SharedPreferenceConstants.SHARED_PREF_MDP, Context.MODE_PRIVATE)
        val sharedPrefMapUpdate = sharedPref.getBoolean(
            SHARED_PREF_MAP_UPDATE,
            DEFAULT_VALUE_MAP_UPDATE
        )
        // if map update is false, display button to update manaully
        if(!sharedPrefMapUpdate){
            button_update_map.apply{
                visibility = View.VISIBLE
                setOnClickListener {
                    Log.d("MainActivity","Update Map")
                }
            }
        }else{
            button_update_map.apply {
                visibility = View.GONE
            }
        }
    }


    private var bluetoothConnectionReceiver:BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context:Context, intent:Intent) {
            val connectionStatus = intent.getIntExtra("ConnectionStatus",99)
            Log.d("MainActivity","Received status :${connectionStatus}")

            Toast.makeText(context, "Connection status: $connectionStatus", Toast.LENGTH_SHORT).show()

            when(connectionStatus){
                BluetoothConstants.STATE_LISTEN -> {
                    supportActionBar?.setSubtitle("Listening for incoming connection")
                }
                BluetoothConstants.STATE_CONNECTED -> {
                    supportActionBar?.setSubtitle("Connected")
                }
                BluetoothConstants.STATE_CONNECTING -> {
                    supportActionBar?.setSubtitle("Connecting")
                }
                BluetoothConstants.STATE_NONE -> {
                    supportActionBar?.setSubtitle("Not connected")
                    unBindService()
                }
                BluetoothConstants.STATE_ERROR -> {
                    supportActionBar?.setSubtitle("Something went wrong")
                }
            }
        }
    }

    private var bluetoothIncomingMessage: BroadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent!!.getStringExtra("IncomingMessage")
            Log.d("MainActivity","Received some message: ${message}")

            val messageParts = message.split(":")

            when(messageParts[0]){
                "map"->{
                    Log.d("MainActivity","message: $messageParts")
                    mazeAdapter.updateMap(Utils().getMapDescriptor(messageParts[1]))
                }
                "pos"->{
                    val idToUpdate = Utils().getRobotPositions(messageParts[1])
                    mMapDescriptor.set(idToUpdate,'3') // 3 is robot head lul
                    mazeAdapter.notifyDataSetChanged()
                }
            }
            //mMapDescriptor = Utils().getMapDescriptor(message)
            //mazeAdapter.updateMap(mMapDescriptor) // update maps when receive data from raspberry pi


            val textMessageReceived = findViewById<TextView>(R.id.text_message_received)
            textMessageReceived.text = message
        }
    }

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder
        ) {
            val binder = service as BService.MyLocalBinder
            myService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false

        }
    }



    private fun initDiscoverBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

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

    }

    /**
     * Sends message to bluetooth service
     * returns a toast if is not binded to bluetooth service
     */
    private fun sendMessageToBluetooth(message: String){
        Log.d("MA","Sending data!")
        if(isBound){
            //myService?.sendMessage(MDPConstants.DEFAULT_MAP_DESCRIPTOR_STRING)
            myService?.sendMessage(message)
        }else{
            Toast.makeText(applicationContext, "Cant send message. No bluetooth connected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        Log.d("MA","onDestroy")
        unregisterReceiver(bluetoothConnectionReceiver)
        unregisterReceiver(bluetoothIncomingMessage)
        unBindService()
        super.onDestroy()

    }

    override fun onResume() {
        Log.d("MA","onResume")
        super.onResume()
    }


    // unbind this activity from service
    private fun unBindService(){
        if (isBound) {
            Log.d("MA","Unbinding service")
            unbindService(myConnection)
            isBound = false
        }
    }


    // inflate the menu item in action bar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    // menu clicks
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.secure_connect_scan -> {
                Log.d("MainActivity","getState: "+myService?.getState())
                if(isBound){
                    if(myService!!.getState() == BluetoothConstants.STATE_CONNECTED){
                        val intent = Intent(this,DisconnectBluetoothActivity::class.java)
                        startActivityForResult(intent, ActivityConstants.REQUEST_BLUETOOTH_DISCONNECT)
                    }else{
                        val intent = Intent(this, DeviceListActivity::class.java)
                        startActivityForResult(intent, ActivityConstants.REQUEST_BLUETOOTH_CONNECTION)
                    }
                }else{
                    // no bluetooth service binded to this activity, hence there is no bluetooth connection.
                    // opens a new activity and display the list of bluetooth macaddress to connect.
                    val intent = Intent(this, DeviceListActivity::class.java)
                    startActivityForResult(intent, ActivityConstants.REQUEST_BLUETOOTH_CONNECTION)
                }
                true
            }
            R.id.menu_function1 -> {
                val sharedPref: SharedPreferences = getSharedPreferences(SharedPreferenceConstants.SHARED_PREF_MDP, Context.MODE_PRIVATE)
                val message = sharedPref.getString(
                    SHARED_PREF_FUNCTION_1,
                    DEFAULT_VALUE_FUNCTION_1
                )
                sendMessageToBluetooth(message)
                true
            }
            R.id.menu_function2 -> {
                val sharedPref: SharedPreferences = getSharedPreferences(SharedPreferenceConstants.SHARED_PREF_MDP, Context.MODE_PRIVATE)
                val message = sharedPref.getString(
                    SHARED_PREF_FUNCTION_2,
                    DEFAULT_VALUE_FUNCTION_2
                )
                sendMessageToBluetooth(message)
                true
            }
            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivityForResult(intent, ActivityConstants.REQUEST_SETTINGS)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            // happens when user taps on a bluetooth macaddress
            ActivityConstants.REQUEST_BLUETOOTH_CONNECTION -> {
                if(resultCode == Activity.RESULT_OK){
                    val address = data?.extras?.getString("device_address")
                    Log.d("MA","requestCode: $requestCode, resultCode: $resultCode, data: $data, device_address: $address")
                    //bluetoothService.connect(bluetoothAdapter.getRemoteDevice(address),mHandler)

                    val intent = Intent(this, BService::class.java)
                    intent.putExtra("serviceType", "connect")
                    intent.putExtra("device",bluetoothAdapter.getRemoteDevice(address))
                    startService(intent)

                    // bind service
                    val intent2 = Intent(this, BService::class.java)
                    bindService(intent2, myConnection, Context.BIND_AUTO_CREATE)
                }
            }
            // happens when user taps on grid in the 2d arena
            ActivityConstants.REQUEST_COORDINATE -> {
                if(resultCode == Activity.RESULT_OK){
                    val xCord = data?.extras?.getString("GRID_NUMBER")
                    val yCord = data?.extras?.getString("")
                    Log.d("MA","requestCode: $requestCode, resultCode: $resultCode, GRID_NUMBER: $xCord")
                    sendMessageToBluetooth("alg:swp,$")
                }
            }
            // happens when user wants to disconnect bluetooth
            ActivityConstants.REQUEST_BLUETOOTH_DISCONNECT -> {
                Log.d("MA","Request bluetooth disconnect")
                if(resultCode == Activity.RESULT_OK){
                    if(isBound){
                        myService?.stop() // stop bluetooth connection
                        unBindService() // unbind service from this activity
                    }
                }
            }

            ActivityConstants.REQUEST_SETTINGS -> {
                Log.d("MA","Request settings")
                if (resultCode == Activity.RESULT_OK){
                    val showMap = data?.extras?.getBoolean("SHOW_MAP_UPDATE_BUTTON")
                    Log.d("MA","requestCode: $requestCode, resultCode: $resultCode, showMap: $showMap")
                    initializeUpdateMap()
                }
            }
        }
    }


}
