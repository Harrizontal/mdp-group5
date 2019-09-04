package com.harrizontal.mdpgroup5.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.harrizontal.mdpgroup5.R
import com.harrizontal.mdpgroup5.bluetooth.BTDevice
import kotlinx.android.synthetic.main.activity_main.view.*

class MazeAxisAdapter(
    private val context: Context,
    private val noOfGrid: Int,
    private val coordinateType: Int
): RecyclerView.Adapter<MazeAxisAdapter.MazeHolder>() {
    private val mItems: IntArray

    init {
        mItems = IntArray(noOfGrid)

        for (i in 0 until noOfGrid)
            mItems[i] = i
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MazeHolder {
        return MazeHolder(LayoutInflater.from(context).inflate(R.layout.list_item_axis_indicator, parent, false))
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun onBindViewHolder(holder: MazeHolder, position: Int) {
        val device = mItems.get(position)
        if(coordinateType == 1){
            // flip the numbers for Y coordinates
            holder.itemView.textView.text = (noOfGrid - 1 - device).toString()
        }else{
            holder.itemView.textView.text = device.toString()
        }

    }


    class MazeHolder(v: View) : RecyclerView.ViewHolder(v){

        companion object {
            //5
            private val PHOTO_KEY = "PHOTO"
        }
    }
}