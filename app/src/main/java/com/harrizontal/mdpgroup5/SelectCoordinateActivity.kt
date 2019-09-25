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

    private lateinit var messageIntent: Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_coordinate)

        val xText = findViewById<TextView>(R.id.x_coordinate)
        val xValue = intent.getStringExtra("X")
        val yValue = intent.getStringExtra("Y")

        xText.text = "($xValue,$yValue)"

        val buttonWayPoint = findViewById<Button>(R.id.button_waypoint)
        val buttonStartCoordinate = findViewById<Button>(R.id.button_start_coordinate)
        messageIntent = Intent(this@SelectCoordinateActivity, MainActivity::class.java)
        messageIntent.putExtra("X", xValue)
        messageIntent.putExtra("Y", yValue)

        buttonWayPoint.setOnClickListener(View.OnClickListener {
            messageIntent.putExtra("REQUEST_COORDINATE_TYPE",REQUEST_WAYPOINT)
            setResult(Activity.RESULT_OK, messageIntent)
            finish()
        })


        buttonStartCoordinate.setOnClickListener {
            val newCoordinates = Utils().recalculateMiddleRobotPosition(xValue.toInt(),yValue.toInt())
            messageIntent.putExtra("REQUEST_COORDINATE_TYPE", REQUEST_START_COORDINATE)
            setResult(Activity.RESULT_OK,messageIntent)
            finish()
        }
    }

}