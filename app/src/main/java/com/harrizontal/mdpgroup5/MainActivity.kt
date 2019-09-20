package com.harrizontal.mdpgroup5

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import android.app.Activity
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonObject
import com.beust.klaxon.KlaxonException
import com.beust.klaxon.Parser
import com.harrizontal.mdpgroup5.adapter.*
import com.harrizontal.mdpgroup5.constants.ActivityConstants
import com.harrizontal.mdpgroup5.constants.ActivityConstants.Companion.REQUEST_START_COORDINATE
import com.harrizontal.mdpgroup5.constants.ActivityConstants.Companion.REQUEST_WAYPOINT
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

class MainActivity : AppCompatActivity(), SensorEventListener {



    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var mMapDescriptor: ArrayList<Char> = ArrayList()
    // actual code
    private val startArea: ArrayList<Int> = MDPConstants.DEFAULT_START_AREA
    private val goalArea: ArrayList<Int> = MDPConstants.DEFAULT_END_AREA
    private var robotPositions: ArrayList<Pair<Int,Pair<Char,Boolean>>> = MDPConstants.DEFAULT_ROBOT_POSITION
    private var wayPointPosition: ArrayList<Int> = ArrayList() // only store one wayPointPosition
    private var imagePositions: ArrayList<Pair<Int,Int>> = ArrayList()

    // for tilt mechanism
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private var enableTilt: Boolean = false

    // delete this once clear checklist
//    private val startArea: ArrayList<Int> = ArrayList()
//    private val goalArea: ArrayList<Int> = ArrayList()
    private var updateMap: Boolean = false


    private lateinit var sharedPref: SharedPreferences
    private lateinit var mazeAdapter: MazeAdapter

    var myService: BService? = null
    var isBound = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.setSubtitle("Not connected")

        registerReceiver(bluetoothConnectionReceiver, IntentFilter("bluetoothConnectionStatus"))
        registerReceiver(bluetoothIncomingMessage,IntentFilter("bluetoothIncomingMessage"))

        sharedPref = getSharedPreferences(
            SharedPreferenceConstants.SHARED_PREF_MDP, Context.MODE_PRIVATE)

        mMapDescriptor = Utils().convertMapDescriptor1ToMapRecycleFormat(MDPConstants.DEFAULT_MAP_DESCRIPTOR_STRING)

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
        mazeAdapter = MazeAdapter(this,MDPConstants.NUM_ROWS,MDPConstants.NUM_COLUMNS,mMapDescriptor,startArea,goalArea,robotPositions,wayPointPosition,imagePositions)
        recycleViewMaze.layoutManager = gridLayoutManager
        recycleViewMaze.adapter = mazeAdapter

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            Log.d("MainActivity","Sensor detected")
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            Toast.makeText(applicationContext,"Sensor detected",Toast.LENGTH_SHORT).show()
        } else {
            Log.d("MainActivity","Sensor not detected")
            Toast.makeText(applicationContext,"No sensor",Toast.LENGTH_SHORT).show()
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

        button_function_1.setOnClickListener {
            val sharedPref: SharedPreferences = getSharedPreferences(SharedPreferenceConstants.SHARED_PREF_MDP, Context.MODE_PRIVATE)
            val message = sharedPref.getString(
                SHARED_PREF_FUNCTION_1,
                DEFAULT_VALUE_FUNCTION_1
            )
            sendMessageToBluetooth(message)
        }

        button_function_2.setOnClickListener {
            val sharedPref: SharedPreferences = getSharedPreferences(SharedPreferenceConstants.SHARED_PREF_MDP, Context.MODE_PRIVATE)
            val message = sharedPref.getString(
                SHARED_PREF_FUNCTION_2,
                DEFAULT_VALUE_FUNCTION_2
            )
            sendMessageToBluetooth(message)
        }


        initializeUpdateMap()

        initDiscoverBluetooth()

        initializeTilt()
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
                    Log.d("MainActivity","Updating Map manually")
                    // actual code
                    val sharedPrefMapUpdate = sharedPref.getBoolean(SHARED_PREF_MAP_UPDATE,DEFAULT_VALUE_MAP_UPDATE)
                    if(!sharedPrefMapUpdate){
                        Log.d("MA","Map updated")
                        mazeAdapter.notifyDataSetChanged()
                    }

                    // for clearing checklist
//                    updateMap = true
//                    sendMessageToBluetooth("sendArena")
                }
            }
        }else{
            button_update_map.apply {
                visibility = View.GONE
            }
        }
    }

    private fun initializeTilt(){
        val sharedPref: SharedPreferences = getSharedPreferences(SharedPreferenceConstants.SHARED_PREF_MDP, Context.MODE_PRIVATE)
        val sharedPrefTilt = sharedPref.getBoolean(
            SharedPreferenceConstants.SHARED_PREF_TILT_MECHANISM,
            SharedPreferenceConstants.DEFAULT_VALUE_TILT
        )
        Log.d("MainActivity","Intialize tilt: $sharedPrefTilt")
        enableTilt = sharedPrefTilt
    }

    private var retryConnectionSecond = 3
    private var previousBluetoothState: Int = BluetoothConstants.STATE_NONE
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
                    //unBindService()
                }
                BluetoothConstants.STATE_ERROR -> {
                    supportActionBar?.setSubtitle("Something went wrong")
                }
                BluetoothConstants.STATE_RETRY_CONNECTING -> {

                    if(retryConnectionSecond < 0){
                        supportActionBar?.setSubtitle("Attempting connection...")
                    }else{
                        supportActionBar?.setSubtitle("Attempting connection in $retryConnectionSecond")
                    }
                    retryConnectionSecond--
                }
            }
            // if the previous state and the current state is the same, reset the connection second to 3.
            if(connectionStatus.equals(previousBluetoothState)){
                retryConnectionSecond = 3
            }



        }
    }

    private var bluetoothIncomingMessage: BroadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent!!.getStringExtra("IncomingMessage")
            Log.d("MainActivity","Received some message: ${message}")

            // for clearing checklist
            val parser: Parser = Parser.default()

            try {
                val stringBuilder: StringBuilder = StringBuilder(message)
                val json = parser.parse(stringBuilder) as JsonObject
                // grid
                if(!(json.string("grid").isNullOrEmpty())){
                    val grid = json.string("grid")
                    val mapDescriptor = Utils().getMapDescriptorsToMapRecycleFormat2(grid!!)

                    val sharedPrefMapUpdate = sharedPref.getBoolean(SHARED_PREF_MAP_UPDATE,DEFAULT_VALUE_MAP_UPDATE)
                    if(sharedPrefMapUpdate || updateMap){
                        Log.d("MainActivity","Updating map")
                        // updates the 2d arena map with robot positions and map descriptor (unexplored, explored, obstacle)
                        mMapDescriptor.clear()
                        mMapDescriptor.addAll(mapDescriptor)
                        mazeAdapter.notifyDataSetChanged()
                        updateMap = false
                    }

                }

                if(!(json.array<Int>("robotPosition").isNullOrEmpty())){
                    val robotPosition = json.array<Int>("robotPosition")

                    val newCoordinate = Utils().flipCoordinatesForADM(robotPosition!!.get(0),robotPosition!!.get(1))
                    val direction = Utils().getDirection(robotPosition!!.get(2))

                    //Log.d("MainActivity","Robot position received ${robotPosition!!.get(0)} - new coordinate: $newCoordinate")
                    robotPositions.clear()
                    robotPositions.addAll(Utils().getRobotPositions2("${newCoordinate.first},${newCoordinate.second},$direction"))
                    mazeAdapter.notifyDataSetChanged()
                }

                if(!(json.string("status").isNullOrEmpty())){
                    val textMessageReceived = findViewById<TextView>(R.id.text_robot_status)
                    val status = json.string("status")
                    textMessageReceived.text = "Robot status: $status"
                }
            }catch (e: KlaxonException){
                val messageParts = message.split(":")

                when(messageParts[0]){
                    "map"->{
                        mMapDescriptor.clear()
                        mMapDescriptor.addAll(Utils().getMapDescriptorsToMapRecycleFormat(messageParts[1]))
                    }
                    "pos"->{
                        robotPositions.clear()
                        robotPositions.addAll(Utils().getRobotPositions(messageParts[1]))
                    }
                    "img"->{
                        val imagePositionAndId = Utils().getImagePosition(messageParts[1])
                        if(imagePositionAndId != null){
                            imagePositions.add(imagePositionAndId)
                        }

                    }
                }
                val sharedPrefMapUpdate = sharedPref.getBoolean(SHARED_PREF_MAP_UPDATE,DEFAULT_VALUE_MAP_UPDATE)
                if(sharedPrefMapUpdate){
                    // updates the 2d arena map with robot positions and map descriptor (unexplored, explored, obstacle)
                    mazeAdapter.notifyDataSetChanged()
                }
            }


            // end of for clearing checklist

            // actual code
//            val messageParts = message.split(":")
//
//            when(messageParts[0]){
//                "map"->{
//                    mMapDescriptor.clear()
//                    mMapDescriptor.addAll(Utils().getMapDescriptorsToMapRecycleFormat(messageParts[1]))
//                }
//                "pos"->{
//                    robotPositions.clear()
//                    robotPositions.addAll(Utils().getRobotPositions(messageParts[1]))
//                }
//            }
//
//            val sharedPrefMapUpdate = sharedPref.getBoolean(SHARED_PREF_MAP_UPDATE,DEFAULT_VALUE_MAP_UPDATE)
//            if(sharedPrefMapUpdate){
//                // updates the 2d arena map with robot positions and map descriptor (unexplored, explored, obstacle)
//                mazeAdapter.notifyDataSetChanged()
//            }

            // end of actual code



            val textMessageReceived = findViewById<TextView>(R.id.text_message_received)
            textMessageReceived.text = "Receive: $message"
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
        sensor?.also { it ->
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
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
                    if(myService!!.getState() == BluetoothConstants.STATE_CONNECTED ||
                            myService!!.getState() == BluetoothConstants.STATE_CONNECTING ||
                            myService!!.getState() == BluetoothConstants.STATE_RETRY_CONNECTING){
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

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
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
                    val xCord = data?.extras?.getString("X")
                    val yCord = data?.extras?.getString("Y")
                    val requestType = data?.extras?.getInt("REQUEST_COORDINATE_TYPE")
                    Log.d("MA","requestCode: $requestCode, resultCode: $resultCode, X:$xCord Y:$yCord")

                    when(requestType){
                        REQUEST_WAYPOINT -> {
                            val id = Utils().convertCoordinatesToGridId(xCord!!.toInt(),yCord!!.toInt())
                            wayPointPosition.clear()
                            wayPointPosition.add(id)
                            mazeAdapter.notifyDataSetChanged()
                            sendMessageToBluetooth("alg:swp,$xCord,$yCord")
                        }
                        REQUEST_START_COORDINATE -> {
                            val newCoordinates = Utils().recalculateMiddleRobotPosition(xCord!!.toInt(),yCord!!.toInt())
                            // for clearing checklist
                            sendMessageToBluetooth("alg:start,$xCord,$yCord")
                            // actual code
                            //sendMessageToBluetooth("alg:start,${newCoordinates.first},${newCoordinates.second}")
                            val robotPositionsString = "${newCoordinates.first},${newCoordinates.second},n" // hardcode to north first
                            robotPositions.clear()
                            robotPositions.addAll(Utils().getRobotPositions(robotPositionsString))
                            mazeAdapter.notifyDataSetChanged() // update the arena map
                        }
                    }
                }
            }
            // happens when user wants to disconnect bluetooth
            ActivityConstants.REQUEST_BLUETOOTH_DISCONNECT -> {
                Log.d("MA","Request bluetooth disconnect")
                if(resultCode == Activity.RESULT_OK){
                    if(isBound){
                        myService?.stopAllConnection() // stopAllConnection bluetooth connection
                        unBindService() // unbind service from this activity
                    }
                }
            }

            ActivityConstants.REQUEST_SETTINGS -> {
                Log.d("MA","Request settings")
                if (resultCode == Activity.RESULT_OK){
                    val showMap = data?.extras?.getBoolean("SHOW_MAP_UPDATE_BUTTON")
                    val tilt = data?.extras?.getBoolean("ENABLE_TILT")
                    if(tilt == false){
                        enableTilt = false
                    }else{
                        enableTilt = true
                    }
                    Log.d("MainActivity","Result - showMap: $showMap, enableTilt: $tilt")
                    initializeUpdateMap()
                }
            }
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        val x = event!!.values[0]
        val y = event!!.values[1]

        //Log.d("onSensorChanged","enableTilt: $enableTilt")
        if(enableTilt) {
            if (Math.abs(x) > Math.abs(y)) {
                // Right Tilt
                if (x < 0) {
                    Log.d("MainActivity:", "RIGHT TILT")
                    sendMessageToBluetooth("mov:right,1")
                }

                // Left Tilt
                if (x > 0) {
                    Log.d("MainActivity:", "LEFT TILT")
                    sendMessageToBluetooth("mov:left,1")
                }
            } else {
                // Forward Tilt
                if (y < 0) {
                    Log.d("MainActivity:", "FORWARD TILT")
                    sendMessageToBluetooth("mov:forward,1")
                }

                // Backward Tilt
                if (y > 0) {
                    //Log.d("MainActivity:", "DOWN TILT!!")
                }
            }
        }
    }

}
