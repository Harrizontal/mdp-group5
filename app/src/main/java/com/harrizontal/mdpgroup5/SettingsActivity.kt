package com.harrizontal.mdpgroup5


import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.SharedPreferences
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_FUNCTION_1
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_FUNCTION_2
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_MAP_UPDATE
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.DEFAULT_VALUE_TILT
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_FUNCTION_1
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_FUNCTION_2
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_MAP_UPDATE
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_MDP
import com.harrizontal.mdpgroup5.constants.SharedPreferenceConstants.Companion.SHARED_PREF_TILT_MECHANISM


class SettingsActivity : AppCompatActivity() {

    private lateinit var textFunction1: TextView
    private lateinit var textFunction2: TextView
    private lateinit var newIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        newIntent = Intent()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initalizeSettings()

    }

    private fun initalizeSettings(){
        val sharedPref: SharedPreferences = getSharedPreferences(SHARED_PREF_MDP, Context.MODE_PRIVATE)


        val sharedPrefMapUpdate = sharedPref.getBoolean(SHARED_PREF_MAP_UPDATE,DEFAULT_VALUE_MAP_UPDATE)
        val sharedPrefTilt = sharedPref.getBoolean(SHARED_PREF_TILT_MECHANISM,DEFAULT_VALUE_TILT)
        val sharedPrefFunction1 = sharedPref.getString(SHARED_PREF_FUNCTION_1,
            DEFAULT_VALUE_FUNCTION_1)
        val sharedPrefFunction2 = sharedPref.getString(SHARED_PREF_FUNCTION_2,DEFAULT_VALUE_FUNCTION_2)

        textFunction1 = findViewById(R.id.text_function_1_extra)
        textFunction2 = findViewById(R.id.text_function_2_extra)

        textFunction1.text = sharedPrefFunction1
        textFunction2.text = sharedPrefFunction2

        val mapUpdateSwitch = findViewById<SwitchMaterial>(R.id.switch_map_update).apply {
            isChecked = sharedPrefMapUpdate
            setOnCheckedChangeListener { buttonView, isChecked ->
                newIntent.putExtra("SHOW_MAP_UPDATE_BUTTON",isChecked)
                setResult(Activity.RESULT_OK,newIntent)

                val editor = sharedPref.edit()
                editor.putBoolean(SHARED_PREF_MAP_UPDATE,isChecked)
                editor.apply()

            }
        }

        val function1Text = findViewById<ConstraintLayout>(R.id.layout_function_1).setOnClickListener {
            showDialogBox("Function 1","Edit command of function 1",SHARED_PREF_FUNCTION_1,
                DEFAULT_VALUE_FUNCTION_1,textFunction1)
        }

        val function2Text = findViewById<ConstraintLayout>(R.id.layout_function_2).setOnClickListener {
            showDialogBox("Function 2","Edit command of function 2",SHARED_PREF_FUNCTION_2,DEFAULT_VALUE_FUNCTION_2,textFunction2)
        }

        val tiltMechanismSwitch = findViewById<SwitchMaterial>(R.id.switch_tilt).apply{
            isChecked = sharedPrefTilt
            setOnCheckedChangeListener{ buttonView, isChecked ->
                newIntent.putExtra("ENABLE_TILT",isChecked)
                setResult(Activity.RESULT_OK,newIntent)
                val editor = sharedPref.edit()
                editor.putBoolean(SHARED_PREF_TILT_MECHANISM,isChecked)
                editor.apply()
            }
        }

        newIntent.putExtra("SHOW_MAP_UPDATE_BUTTON",mapUpdateSwitch.isChecked)
        newIntent.putExtra("ENABLE_TILT",tiltMechanismSwitch.isChecked)
        setResult(Activity.RESULT_OK,newIntent)

    }

    private fun showDialogBox(title: String, message: String, sharedPrefConstant: String, sharedPrefDefault: String, textView: TextView){
        val sharedPref: SharedPreferences = getSharedPreferences(SHARED_PREF_MDP, Context.MODE_PRIVATE)
        val sharedPrefCommand = sharedPref.getString(sharedPrefConstant,sharedPrefDefault)

        val alert = AlertDialog.Builder(this)
        val edittext = EditText(applicationContext)
        edittext.setText(sharedPrefCommand)
        alert.setMessage(message)
        alert.setTitle(title)

        alert.setView(edittext)

        alert.setPositiveButton("Save"
        ) { dialog, whichButton ->
            val editedText = edittext.text.toString()
            if(!(editedText.equals(sharedPrefCommand))){
                Log.d("SettingsActivity","text: $editedText")

                // edit the shared preference
                val editor = sharedPref.edit()
                editor.putString(sharedPrefConstant,editedText)
                editor.apply()

                // update the textview in the activity
                textView.setText(editedText)

                Toast.makeText(applicationContext, "$title's command updated", Toast.LENGTH_SHORT).show()
            }
        }

        alert.setNegativeButton("Cancel"
        ) { dialog, whichButton ->
            // what ever you want to do with No option.
        }

        alert.show()
    }


    override fun onDestroy() {
        Log.d("SettingsActivity","onDestory")
        super.onDestroy()

    }

    override fun onResume() {
        Log.d("SettingsActivity","onResume")
        super.onResume()
    }

}