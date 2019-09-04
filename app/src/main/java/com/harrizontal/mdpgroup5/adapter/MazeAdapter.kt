package com.harrizontal.mdpgroup5.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.harrizontal.mdpgroup5.R
import com.harrizontal.mdpgroup5.SelectCoordinateActivity
import com.harrizontal.mdpgroup5.constants.ActivityConstants
import com.harrizontal.mdpgroup5.constants.MDPConstants
import kotlinx.android.synthetic.main.activity_main.view.*

class MazeAdapter(
    private val context: Context,
    private val rows: Int,
    private val columns: Int,
    private var mapDescriptor: ArrayList<Char>
): RecyclerView.Adapter<MazeAdapter.MazeHolder>() {
    private val mItems: IntArray

    init {
        mItems = IntArray(rows*columns)

        for (i in 0 until (rows*columns))
            mItems[i] = i
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MazeHolder {
        return MazeHolder(LayoutInflater.from(context).inflate(R.layout.list_item_grid_box, parent, false))
    }

    override fun getItemCount(): Int {
        return rows*columns
    }

    override fun onBindViewHolder(holder: MazeHolder, position: Int) {
        var blockType: Char?  = mapDescriptor.getOrNull(position)
        if(blockType == null) {blockType = '0'}

        val xCoord = position % columns
        val yCoord = (rows - 1 - (position / columns))

        Log.d("MazeAdapter","Update block to $blockType. mapDescriptor.size: ${mapDescriptor.size}")
        // update background based on type of block
        when(blockType.toString()){
            MDPConstants.UNEXPLORED -> {
                holder.itemView.setBackgroundResource(R.drawable.cell_item_unexplored)
            }
            MDPConstants.EXPLORED -> {
                holder.itemView.setBackgroundResource(R.drawable.cell_item_explored)
            }
            MDPConstants.OBSTACLE -> {
                holder.itemView.setBackgroundResource(R.drawable.cell_item_obstacle)
            }
            MDPConstants.ROBOT_HEAD -> {
                holder.itemView.setBackgroundResource(R.drawable.cell_item_robot_head)
            }
            MDPConstants.ROBOT_BODY -> {
                holder.itemView.setBackgroundResource(R.drawable.cell_item_robot_body)
            }
        }

        val device = mItems.get(position)
        holder.itemView.textView.text = xCoord.toString() + "," + yCoord.toString()


        holder.itemView.setOnClickListener {
            Log.d("MazeAdapter","grid id: $device")
            val intent = Intent(context, SelectCoordinateActivity::class.java)
            intent.putExtra("X",device)
            (context as Activity).startActivityForResult(
                intent,
                ActivityConstants.REQUEST_COORDINATE
            )
        }
    }


    class MazeHolder(v: View) : RecyclerView.ViewHolder(v){

        companion object {
            //5
            private val PHOTO_KEY = "PHOTO"
        }
    }

    fun updateMap(mMapDescriptor: ArrayList<Char>){
        Log.d("MazeAdapter","Uploading map with ${mMapDescriptor.size} characters")
        mapDescriptor.clear()
        mapDescriptor.addAll(mMapDescriptor)
        notifyDataSetChanged()
    }
}