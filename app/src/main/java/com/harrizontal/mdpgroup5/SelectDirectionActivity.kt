package com.harrizontal.mdpgroup5

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.harrizontal.mdpgroup5.constants.ActivityConstants.Companion.REQUEST_DIRECTION
import com.harrizontal.mdpgroup5.constants.ActivityConstants.Companion.REQUEST_START_COORDINATE
import com.harrizontal.mdpgroup5.constants.ActivityConstants.Companion.REQUEST_WAYPOINT
import com.harrizontal.mdpgroup5.helper.Utils
import kotlinx.android.synthetic.main.activity_select_coordinate.*
import kotlinx.android.synthetic.main.activity_select_coordinate.button_cancel
import kotlinx.android.synthetic.main.activity_select_coordinate.button_close
import kotlinx.android.synthetic.main.activity_select_direction.*

class SelectDirectionActivity : Activity() {

    private lateinit var messageIntent: Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_direction)

        val textCoordinate = findViewById<TextView>(R.id.text_coordinate)
        val xValue = intent.getStringExtra("X")
        val yValue = intent.getStringExtra("Y")

        val coordinate = "Set direction for robot at ($xValue,$yValue)"
        textCoordinate.text = coordinate

        messageIntent = Intent(this@SelectDirectionActivity, MainActivity::class.java)
        messageIntent.putExtra("X", xValue)
        messageIntent.putExtra("Y", yValue)



        button_west.setOnClickListener {
            messageIntent.putExtra("REQUEST_COORDINATE_TYPE", REQUEST_DIRECTION)
            messageIntent.putExtra("direction","w")
            setResult(Activity.RESULT_OK,messageIntent)
            finish()
        }

        button_north.setOnClickListener {
            messageIntent.putExtra("REQUEST_COORDINATE_TYPE", REQUEST_DIRECTION)
            messageIntent.putExtra("direction","n")
            setResult(Activity.RESULT_OK,messageIntent)
            finish()
        }

        button_south.setOnClickListener {
            messageIntent.putExtra("REQUEST_COORDINATE_TYPE", REQUEST_DIRECTION)
            messageIntent.putExtra("direction","s")
            setResult(Activity.RESULT_OK,messageIntent)
            finish()
        }

        button_east.setOnClickListener {
            messageIntent.putExtra("REQUEST_COORDINATE_TYPE", REQUEST_DIRECTION)
            messageIntent.putExtra("direction","e")
            setResult(Activity.RESULT_OK,messageIntent)
            finish()
        }

        button_close.setOnClickListener(closeDialogBox)
    }

    private var closeDialogBox = object: View.OnClickListener{
        override fun onClick(v: View?) {
           finish()
        }
    }

}