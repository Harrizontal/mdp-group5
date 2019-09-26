package com.harrizontal.mdpgroup5

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.Activity
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harrizontal.mdpgroup5.adapter.ImagesFoundRecycleAdapter
import com.harrizontal.mdpgroup5.constants.ActivityConstants
import com.harrizontal.mdpgroup5.constants.ActivityConstants.Companion.REQUEST_START_COORDINATE
import com.harrizontal.mdpgroup5.constants.ActivityConstants.Companion.REQUEST_WAYPOINT
import com.harrizontal.mdpgroup5.constants.BluetoothConstants
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_FUNCTION_1
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_FUNCTION_2
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_MAP_UPDATE
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_FUNCTION_1
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_FUNCTION_2
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_MAP_UPDATE
import com.harrizontal.mdpgroup5.helper.Utils
import com.harrizontal.mdpgroup5.service.BService
import kotlinx.android.synthetic.main.activity_main_3.*


class MainActivity : AppCompatActivity(), SensorEventListener {



    private lateinit var bluetoothAdapter: BluetoothAdapter


    // for tilt mechanism
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private var enableTilt: Boolean = false

    // shared preference
    private lateinit var sharedPref: SharedPreferences

    // arena map
    private lateinit var arenaGridView: ArenaGridView

    var myService: BService? = null
    var isBound = false


    private lateinit var percentageExploredTextView: TextView


    // for images found UI
    private lateinit var imagesFoundRecycleView: RecyclerView
    private var imagesFound: ArrayList<String> = ArrayList()
    private lateinit var imagesFoundRecycleAdapter: ImagesFoundRecycleAdapter
    // for timer
    private lateinit var timerTextView: TextView

    val timerHandler = Handler()
    var startTime: Long = 0
    var timerRunnable: Runnable = object : Runnable {

        override fun run() {
            val millis = System.currentTimeMillis() - startTime
            var seconds = (millis / 1000).toInt()
            val minutes = seconds / 60
            seconds = seconds % 60

            timerTextView.text = (String.format("%d:%02d", minutes, seconds))
            //Log.d("Timer",((String.format("%d:%02d", minutes, seconds))))
            timerHandler.postDelayed(this,500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_3)

        supportActionBar?.setSubtitle("Not connected")

        registerReceiver(bluetoothConnectionReceiver, IntentFilter("bluetoothConnectionStatus"))
        registerReceiver(bluetoothIncomingMessage,IntentFilter("bluetoothIncomingMessage"))

        sharedPref = getSharedPreferences(
            SharedPreferenceConstants.SHARED_PREF_MDP, Context.MODE_PRIVATE)

        arenaGridView = findViewById<ArenaGridView>(R.id.arenagridview)

        timerTextView = findViewById(R.id.text_timer)
        percentageExploredTextView = findViewById(R.id.text_exploration_percent)

        imagesFoundRecycleView = findViewById(R.id.recyclerview_image_found)
        imagesFoundRecycleView.layoutManager = LinearLayoutManager(this)
        imagesFoundRecycleAdapter = ImagesFoundRecycleAdapter(imagesFound,this)
        imagesFoundRecycleView.adapter = imagesFoundRecycleAdapter

        // sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            Log.d("MainActivity","Sensor detected")
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            Toast.makeText(applicationContext,"Sensor detected",Toast.LENGTH_SHORT).show()
        } else {
            Log.d("MainActivity","Sensor not detected")
            Toast.makeText(applicationContext,"No sensor",Toast.LENGTH_SHORT).show()
        }

        button_turnleft?.setOnClickListener {
            sendMessageToBluetooth("mov:left,1")
        }

        button_turnRight.setOnClickListener {
            sendMessageToBluetooth("mov:right,1")
        }

        button_up.setOnClickListener {
            sendMessageToBluetooth("mov:forward,1")
        }

        button_exploration.setOnClickListener {
            if(sendMessageToBluetooth("alg:explore")){
                Toast.makeText(applicationContext, "Exploration started", Toast.LENGTH_SHORT).show()
            }
            timerHandler.removeCallbacks(timerRunnable) // stop timer
            startTime = System.currentTimeMillis()
            timerHandler.postDelayed(timerRunnable, 0) // start timer
            percentageExploredTextView.text = "0%" // percentage set to 0
            arenaGridView.resetMap() // reset the map
            imagesFound.clear() // clear images
            imagesFoundRecycleAdapter.notifyDataSetChanged()
        }

        button_fastest_path.setOnClickListener {
            if(sendMessageToBluetooth("alg:fast")){
                Toast.makeText(applicationContext, "Fastest path started", Toast.LENGTH_SHORT).show()
            }
            timerHandler.removeCallbacks(timerRunnable) // stop timer
            startTime = System.currentTimeMillis()
            timerHandler.postDelayed(timerRunnable, 0) // start timer
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
        arenaGridView.setAutoUpdate(sharedPrefMapUpdate)

        // if map update is false, display button to update manaully
        if(!sharedPrefMapUpdate){
            button_update_map.apply{
                visibility = View.VISIBLE
                setOnClickListener {
                    val sharedPrefMapUpdate = sharedPref.getBoolean(SHARED_PREF_MAP_UPDATE,DEFAULT_VALUE_MAP_UPDATE)
                    if(!sharedPrefMapUpdate){
                        Log.d("MA","Map updated")
                        // update map
                        arenaGridView.invalidate()
                    }
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

            val messageParts = message.split(":")

            val textMessageReceived = findViewById<TextView>(R.id.text_message_received)
            textMessageReceived.text = "Receive: $message"

            when(messageParts[0]){
                "map"->{
                    val mapDescriptor = Utils().getcombinedMapDescriptor(messageParts[1])
                    arenaGridView.updateExploredAndObstacles(mapDescriptor) // update grids
                    val percentageExplored = Utils().countExploredPercentage(mapDescriptor)
                    percentageExploredTextView.text = "${percentageExplored}%" // update percentage in UI
                    // stops timer when its 100% explored
                    if(percentageExplored == 100){
                        timerHandler.removeCallbacks(timerRunnable)
                    }
                }
                "pos"->{
                    val messageInnerParts = messageParts[1].split(",")
                    val x = messageInnerParts[0].toInt()
                    val y = messageInnerParts[1].toInt()
                    arenaGridView.setRobot(Pair(x,y),messageInnerParts[2])
                }
                "img"->{
                    val messageInnerParts = messageParts[1].split(",")
                    arenaGridView.addDetectedImages(Pair(Pair(messageInnerParts[0].toInt(),messageInnerParts[1].toInt()),messageInnerParts[2].toInt()))
                    // updates the recyclerview for images found
                    val imageString = "(${messageInnerParts[2]},${messageInnerParts[0]},${messageInnerParts[1]})"
                    imagesFound.add(imageString)
                    imagesFoundRecycleAdapter.notifyDataSetChanged()


                }
            }

            val sharedPrefMapUpdate = sharedPref.getBoolean(SHARED_PREF_MAP_UPDATE,DEFAULT_VALUE_MAP_UPDATE)
            if(sharedPrefMapUpdate){
                // updates the 2d arena map with robot positions and map descriptor (unexplored, explored, obstacle)
               //mazeAdapter.notifyDataSetChanged()
            }


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
    private fun sendMessageToBluetooth(message: String) : Boolean{
        Log.d("MA","Sending data!")
        if(isBound){
            //myService?.sendMessage(MDPConstants.DEFAULT_MAP_DESCRIPTOR_STRING)
            myService?.sendMessage(message)
            return true
        }else{
            Toast.makeText(applicationContext, "Cant send message. No bluetooth connected", Toast.LENGTH_SHORT).show()
            return false
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
            R.id.menu_reset_map -> {
                timerHandler.removeCallbacks(timerRunnable) // stop timer
                timerTextView.text ="0:00" // set 0:00
                percentageExploredTextView.text = "0%"
                arenaGridView.resetMap()
                arenaGridView.invalidate() // force to update arena map even though consistent map update is turned off
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
                            arenaGridView.setWaypoint(Pair(xCord!!.toInt(),yCord!!.toInt()))
                            sendMessageToBluetooth("alg:swp,$yCord,$xCord") // sending row,column
                        }
                        REQUEST_START_COORDINATE -> {
                            val newCoordinates = Utils().recalculateMiddleRobotPosition(xCord!!.toInt(),yCord!!.toInt()) // gives x,y as a pair
                            arenaGridView.setRobot(Pair(newCoordinates.first,newCoordinates.second),"n") // hard code north
                            sendMessageToBluetooth("alg:start,${newCoordinates.second},${newCoordinates.first}")  // sending row,column
                        }
                    }
                }

                // remove touch effect on grid
                arenaGridView.removeTouchEffect()
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

                }
            }
        }
    }

}
