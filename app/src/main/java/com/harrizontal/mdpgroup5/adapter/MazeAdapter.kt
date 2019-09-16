package com.harrizontal.mdpgroup5.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.harrizontal.mdpgroup5.SelectCoordinateActivity
import com.harrizontal.mdpgroup5.constants.ActivityConstants
import com.harrizontal.mdpgroup5.constants.MDPConstants
import kotlinx.android.synthetic.main.list_item_grid_box.view.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.harrizontal.mdpgroup5.R
import android.graphics.PorterDuff


class MazeAdapter(
    private val context: Context,
    private val rows: Int,
    private val columns: Int,
    private var mapDescriptor: ArrayList<Char>,
    private var startArea: ArrayList<Int>, // ArrayList of start and end position
    private var goalArea: ArrayList<Int>,
    private var robotPositions: ArrayList<Pair<Int,Pair<Char,Boolean>>>,
    private var wayPoint: ArrayList<Int>
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

        // update background based on type of block
        when(blockType){
            MDPConstants.UNEXPLORED -> {
                holder.itemView.setBackgroundResource(R.drawable.cell_item_unexplored)
            }
            MDPConstants.EXPLORED -> {
                holder.itemView.setBackgroundResource(R.drawable.cell_item_explored)
            }
            MDPConstants.OBSTACLE -> {
                holder.itemView.setBackgroundResource(R.drawable.cell_item_obstacle)
            }
        }

        // setting up start area in map
        for (x in 0 until startArea.size){
            if(position == startArea.get(x)){
                holder.itemView.textView.setTextColor(Color.BLUE)
                holder.itemView.view_shaded.setBackgroundColor(context.resources.getColor(R.color.colorStartArena))
                break
            }
        }

        // setting up goal area in map
        for (x in 0 until goalArea.size){
            if(position == goalArea.get(x)){
                holder.itemView.textView.setTextColor(Color.GREEN)
                holder.itemView.view_shaded.setBackgroundColor(context.resources.getColor(R.color.colorGoalArena))
                break
            }
        }
        //Fix a bug where robot display in the first few items in the recycleview
        holder.itemView.image_robot.visibility = View.GONE

        // setting up robot position in the map
        for (k in 0 until robotPositions.size){
            if(position == robotPositions.get(k).first){
                Log.d("MazeAdapter","Updating $position")
                holder.itemView.image_robot.visibility = View.VISIBLE
                generateRobotPart(robotPositions.get(k).second.first,holder.itemView,robotPositions.get(k).second.second)
                break
            }
        }

        // setting up waypoint
        if(wayPoint.size != 0){
            if(position == wayPoint.get(0)){
                Log.d("MazeAdapter","Waypoint detected")
                holder.itemView.image_waypoint.setBackgroundColor(context.resources.getColor(R.color.colorWayPoint))
            }
        }


        // set text with coordinates and set clicklistener to grid
        val grid = mItems.get(position)
        holder.itemView.textView.text = xCoord.toString() + "," + yCoord.toString()
        holder.itemView.setOnClickListener {
            Log.d("MazeAdapter","grid id: $grid")
            val intent = Intent(context, SelectCoordinateActivity::class.java)
            intent.putExtra("GRID_NUMBER",grid)
            intent.putExtra("X",xCoord.toString())
            intent.putExtra("Y",yCoord.toString())
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


    /**
     * Generate robot body and robot head for 2D Arena
     */
    private fun generateRobotPart(robotPart: Char, itemView: View,isHead: Boolean){
        val set = ConstraintSet()
        val layout: ConstraintLayout = itemView.findViewById(R.id.layout_grid) as ConstraintLayout
        set.clone(layout)
        set.clear(R.id.image_robot,ConstraintSet.LEFT)
        set.clear(R.id.image_robot,ConstraintSet.RIGHT)
        set.clear(R.id.image_robot,ConstraintSet.TOP)
        set.clear(R.id.image_robot,ConstraintSet.BOTTOM)
        when(robotPart){
            MDPConstants.ROBOT_TOP_LEFT -> {
                itemView.image_robot.setImageDrawable(context.resources.getDrawable(R.drawable.cell_robot_topleft))
                set.connect(R.id.image_robot,ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0)
                set.connect(R.id.image_robot,ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0)
            }
            MDPConstants.ROBOT_TOP -> {
                itemView.image_robot.setImageDrawable(context.resources.getDrawable(R.drawable.cell_robot_topbottom))
                set.connect(R.id.image_robot,ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0)
                set.connect(R.id.image_robot,ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0)
                set.connect(R.id.image_robot,ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0)
            }
            MDPConstants.ROBOT_TOP_RIGHT ->{
                itemView.image_robot.setImageDrawable(context.resources.getDrawable(R.drawable.cell_robot_topright))
                set.connect(R.id.image_robot,ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0)
                set.connect(R.id.image_robot,ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0)
            }
            MDPConstants.ROBOT_MIDDLE_LEFT -> {
                itemView.image_robot.setImageDrawable(context.resources.getDrawable(R.drawable.cell_robot_middle_side))
                set.connect(R.id.image_robot,ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0)
                set.connect(R.id.image_robot,ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0)
                set.connect(R.id.image_robot,ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0)
            }
            MDPConstants.ROBOT_MIDDLE -> {
                itemView.image_robot.setImageDrawable(context.resources.getDrawable(R.drawable.cell_robot_middle))
                set.connect(R.id.image_robot,ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0)
                set.connect(R.id.image_robot,ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0)
                set.connect(R.id.image_robot,ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0)
                set.connect(R.id.image_robot,ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0)
            }
            MDPConstants.ROBOT_MIDDLE_RIGHT -> {
                itemView.image_robot.setImageDrawable(context.resources.getDrawable(R.drawable.cell_robot_middle_side))
                set.connect(R.id.image_robot,ConstraintSet.BOTTOM,ConstraintSet.PARENT_ID,ConstraintSet.BOTTOM,0)
                set.connect(R.id.image_robot,ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0)
                set.connect(R.id.image_robot,ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0)
            }
            MDPConstants.ROBOT_BOTTOM_LEFT ->{
                itemView.image_robot.setImageDrawable(context.resources.getDrawable(R.drawable.cell_robot_bottomleft))
                set.connect(R.id.image_robot,ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0)
                set.connect(R.id.image_robot,ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0)
            }
            MDPConstants.ROBOT_BOTTOM ->{
                itemView.image_robot.setImageDrawable(context.resources.getDrawable(R.drawable.cell_robot_topbottom))
                set.connect(R.id.image_robot,ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0)
                set.connect(R.id.image_robot,ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0)
                set.connect(R.id.image_robot,ConstraintSet.RIGHT,ConstraintSet.PARENT_ID,ConstraintSet.RIGHT,0)
            }
            MDPConstants.ROBOT_BOTTOM_RIGHT ->{
                itemView.image_robot.setImageDrawable(context.resources.getDrawable(R.drawable.cell_robot_bottomright))
                set.connect(R.id.image_robot,ConstraintSet.LEFT,ConstraintSet.PARENT_ID,ConstraintSet.LEFT,0)
                set.connect(R.id.image_robot,ConstraintSet.TOP,ConstraintSet.PARENT_ID,ConstraintSet.TOP,0)
            }
        }

        //itemView.image_robot.visibility = View.VISIBLE

        if(isHead){
            itemView.image_robot.setColorFilter(Color.BLACK, PorterDuff.Mode.OVERLAY)
        }

        set.applyTo(layout)
    }
}