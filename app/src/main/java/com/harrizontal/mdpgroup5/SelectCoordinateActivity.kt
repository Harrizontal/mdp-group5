package com.harrizontal.mdpgroup5

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

class SelectCoordinateActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_coordinate)

        val xText = findViewById<TextView>(R.id.x_coordinate)
        val xCoord = intent.getIntExtra("X", 0)
        xText.text = xCoord.toString()

        val buttonWayPoint = findViewById<Button>(R.id.waypoint_btn)

        buttonWayPoint.setOnClickListener(View.OnClickListener {
            val messageIntent = Intent(this@SelectCoordinateActivity, MainActivity::class.java)
            messageIntent.putExtra("X", xCoord.toString())
//            messageIntent.putExtra("Y", yCoord)
//            messageIntent.putExtra("TYPE", "wayPoint")
            setResult(Activity.RESULT_OK, messageIntent)
            finish()
        })
    }

}