package com.harrizontal.mdpgroup5

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.harrizontal.mdpgroup5.constants.ActivityConstants.Companion.REQUEST_START_COORDINATE
import com.harrizontal.mdpgroup5.constants.ActivityConstants.Companion.REQUEST_WAYPOINT
import com.harrizontal.mdpgroup5.helper.Utils

class SelectCoordinateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_coordinate)

        val xText = findViewById<TextView>(R.id.x_coordinate)
        val gridNumber = intent.getIntExtra("GRID_NUMBER", 0) // get grid id (recycleview's item id)
        val xValue = intent.getStringExtra("X")
        val yValue = intent.getStringExtra("Y")

        xText.text = "($xValue,$yValue)"

        val buttonWayPoint = findViewById<Button>(R.id.button_waypoint)
        val buttonStartCoordinate = findViewById<Button>(R.id.button_start_coordinate)

        buttonWayPoint.setOnClickListener(View.OnClickListener {
            val messageIntent = Intent(this@SelectCoordinateActivity, MainActivity::class.java)
            messageIntent.putExtra("GRID_NUMBER", gridNumber.toString())
            messageIntent.putExtra("X", xValue)
            messageIntent.putExtra("Y", yValue)
            messageIntent.putExtra("REQUEST_COORDINATE_TYPE",REQUEST_WAYPOINT)
            setResult(Activity.RESULT_OK, messageIntent)
            finish()
        })

        // hard coded coordinates :(
        // will edit soon if can
        buttonStartCoordinate.setOnClickListener {


            val newCoordinates = Utils().recalculateMiddleRobotPosition(xValue.toInt(),yValue.toInt())

            val messageIntent = Intent(this@SelectCoordinateActivity, MainActivity::class.java)
            messageIntent.putExtra("GRID_NUMBER", gridNumber.toString())
            messageIntent.putExtra("X", newCoordinates.first.toString())
            messageIntent.putExtra("Y", newCoordinates.second.toString())
            messageIntent.putExtra("REQUEST_COORDINATE_TYPE", REQUEST_START_COORDINATE)
            setResult(Activity.RESULT_OK,messageIntent)
            finish()
        }
    }

}