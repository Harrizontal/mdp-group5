package com.harrizontal.mdpgroup5.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.harrizontal.mdpgroup5.R
import kotlinx.android.synthetic.main.list_item_bluetooth.view.*
import android.app.Activity
import android.content.Intent
import com.harrizontal.mdpgroup5.bluetooth.BTDevice


class RecycleAdapter(
    private val listBluetooth: ArrayList<BTDevice>,
    private val context: Context
): RecyclerView.Adapter<RecycleAdapter.BluetoothHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothHolder {
        return BluetoothHolder(LayoutInflater.from(context).inflate(R.layout.list_item_bluetooth, parent, false))
    }

    override fun getItemCount(): Int {
        return listBluetooth.size
    }

    override fun onBindViewHolder(holder: BluetoothHolder, position: Int) {
        val device = listBluetooth.get(position)
        holder.itemView.name.text = device.name
        holder.itemView.address.text = device.address
        holder.itemView.setOnClickListener {
            Log.d("RA","You clicked on ${device.address}")
            val intent = Intent()
            intent.putExtra("device_address",device.address)
            (context as Activity).setResult(Activity.RESULT_OK,intent)
            context.finish()
        }
    }


    class BluetoothHolder(v: View) : RecyclerView.ViewHolder(v){

        companion object {
            //5
            private val PHOTO_KEY = "PHOTO"
        }
    }
}