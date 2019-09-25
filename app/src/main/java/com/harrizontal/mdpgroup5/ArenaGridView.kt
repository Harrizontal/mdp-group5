package com.harrizontal.mdpgroup5

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.text.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.harrizontal.mdpgroup5.constants.ActivityConstants
import com.harrizontal.mdpgroup5.constants.MDPConstants
import androidx.core.content.ContextCompat




class ArenaGridView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {


    private var numberRows = MDPConstants.NUM_ROWS // y
    private var numberColumns = MDPConstants.NUM_COLUMNS // x
    private var cellWidth: Int = 0 // for generating the grid
    private var cellHeight: Int = 0

    private var mapDescriptor: ArrayList<Char> = ArrayList()
    private var robot: Pair<Int,Int> = Pair(1,1)
    private var robotDirection: String = "n"
    private var startZone: ArrayList<Pair<Int,Int>> = ArrayList()
    private var endZone: ArrayList<Pair<Int,Int>> = ArrayList()
    private var images: ArrayList<Pair<Pair<Int,Int>,Int>> = ArrayList()
    private var waypoint: Pair<Int,Int>? = null
    private var autoUpdate: Boolean = true
    private var touchRectangle: Pair<Int,Int>? = null
    private var exploredCount: Int = 0 // for percentage explored UI



    private var paintUnexplored = Paint().also {
        it.color = getColor(R.color.colorUnexplored)
    }

    private var paintObstacle = Paint().also {
        it.color = getColor(R.color.colorObstacle)
    }

    private var paintExplored = Paint().also{
        it.color = getColor(R.color.colorExplored)
    }

    private var paintStart = Paint().also {
        it.color = getColor(R.color.colorStartArena)
    }

    private var paintEnd = Paint().also{
        it.color = getColor(R.color.colorGoalArena)
    }

    private var paintRobot = Paint().also {
        it.color = getColor(R.color.colorRobotBody)
    }

    private var paintRobotHead = Paint().also {
        it.color = getColor(R.color.colorRobotHead)
    }

    private var paintWaypoint = Paint().also {
        it.color = getColor(R.color.colorWayPoint)
    }

    private var paintTouchEffect = Paint().also{
        it.color = getColor(R.color.colorTransparent)
    }


    init {
        calculateCells()
        // adding coordinates for start and end zone
        populateStartEndZone()

    }

    private fun getColor(id: Int): Int{
        return ContextCompat.getColor(context,id)
    }

    private fun populateStartEndZone(){
        startZone.add(Pair(0,0))
        startZone.add(Pair(1,0))
        startZone.add(Pair(2,0))
        startZone.add(Pair(0,1))
        startZone.add(Pair(1,1))
        startZone.add(Pair(2,1))
        startZone.add(Pair(0,2))
        startZone.add(Pair(1,2))
        startZone.add(Pair(2,2))
        endZone.add(Pair(12,19))
        endZone.add(Pair(13,19))
        endZone.add(Pair(14,19))
        endZone.add(Pair(12,18))
        endZone.add(Pair(13,18))
        endZone.add(Pair(14,18))
        endZone.add(Pair(12,17))
        endZone.add(Pair(13,17))
        endZone.add(Pair(14,17))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateCells()
    }

    private fun calculateCells(){
        Log.d("ArenaGridView","width: $width")
        cellWidth = width / numberColumns
        cellHeight = cellWidth
    }

    override fun onDraw(canvas: Canvas) {
        drawBasicGridArena(canvas)
        drawMapDescriptor(canvas)
        drawGridLines(canvas)
        drawStartAndEndZone(canvas)
        drawWaypoint(canvas)
        drawCoordinates(canvas)
        drawRobot(canvas)
        drawDetectedImages(canvas)
        drawTouchEffect(canvas)
    }

    private fun drawBasicGridArena(canvas: Canvas){
        for (i in 0 until numberColumns) {
            for (j in 0 until numberRows) {
                canvas.drawRect(
                    (i * cellWidth).toFloat(), (j * cellHeight).toFloat(),
                    ((i + 1) * cellWidth).toFloat(), ((j + 1) * cellHeight).toFloat(),
                    paintUnexplored
                )
            }
        }
    }

    private fun drawCoordinates(canvas: Canvas){
        for (i in 0 until numberColumns) {
            for (j in 0 until numberRows) {
                val left = (i * cellWidth).toFloat()
                val top = (j * cellHeight).toFloat()
                val right = ((i + 1) * cellWidth).toFloat()
                val bottom = ((j + 1) * cellHeight).toFloat()
                val x = left + ((right - left)/2)
                val y = top + ((bottom - top)/1.7)


                val myTextPaint = TextPaint()
                myTextPaint.setAntiAlias(true)
                myTextPaint.setTextSize(8 * resources.displayMetrics.density)
                myTextPaint.setColor(-0x1000000)
                myTextPaint.textAlign = Paint.Align.CENTER
                val yCoordinate = MDPConstants.NUM_ROWS - 1 - j
                val coordinateText = "$i,$yCoordinate"
                canvas.drawText(coordinateText,x,y.toFloat(),myTextPaint)
            }
        }
    }

    private fun drawGridLines(canvas: Canvas){
        // draw vertical grid lines
        for (i in 1 until numberColumns) {
            canvas.drawLine(
                (i * cellWidth).toFloat(),
                0f,
                (i * cellWidth).toFloat(),
                height.toFloat(),
                paintObstacle
            )
        }

        // horizontal lines
        for (i in 1 until numberRows) {
            canvas.drawLine(
                0f,
                (i * cellHeight).toFloat(),
                (numberColumns*cellWidth).toFloat(),
                (i * cellHeight).toFloat(),
                paintObstacle
            )
        }


    }

    private fun drawStartAndEndZone(canvas: Canvas){
        // draw start and end zone (both 3x3)
        for (i in 0 until numberColumns) {
            for (j in 0 until numberRows) {
                for (item in startZone){
                    if(item.first == i && item.second == j){
                        canvas.drawRect(
                            (i * cellWidth).toFloat(), ((numberRows-1-j) * cellHeight).toFloat(),
                            ((i + 1) * cellWidth).toFloat(), ((numberRows - j) * cellHeight).toFloat(),
                            paintStart
                        )
                        break
                    }
                }
                for (item in endZone){
                    if(item.first == i && item.second == j){
                        canvas.drawRect(
                            (i * cellWidth).toFloat(), ((numberRows-1-j) * cellHeight).toFloat(),
                            ((i + 1) * cellWidth).toFloat(), ((numberRows - j) * cellHeight).toFloat(),
                            paintEnd
                        )
                        break
                    }
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val calculatedWeight = MeasureSpec.getSize(widthMeasureSpec)
        Log.d("ArenaGridView","calculatedWeight: $calculatedWeight , width: $width")
        val calculatedHeight = MeasureSpec.getSize(widthMeasureSpec) / numberColumns * numberRows
        setMeasuredDimension(calculatedWeight,calculatedHeight)
    }


    // draw obstacles, unexplored and explored
    private fun drawMapDescriptor(canvas: Canvas){
        var x = 0
        for (i in 0 until numberRows) {
            for (j in 0 until numberColumns) {
                if(x < mapDescriptor.size){
                    when(mapDescriptor.get(x)){
                        MDPConstants.OBSTACLE -> {
                            canvas.drawRect(
                                (j * cellWidth).toFloat(), ((numberRows-1-i) * cellHeight).toFloat(),
                                ((j + 1) * cellWidth).toFloat(), ((numberRows - i) * cellHeight).toFloat(),
                                paintObstacle
                            )
                            exploredCount++ // for percentaged explored UI
                        }
                        MDPConstants.EXPLORED -> {
                            canvas.drawRect(
                                (j * cellWidth).toFloat(), ((numberRows-1-i) * cellHeight).toFloat(),
                                ((j + 1) * cellWidth).toFloat(), ((numberRows - i) * cellHeight).toFloat(),
                                paintExplored
                            )
                            exploredCount++ // for percentaged explored UI
                        }
                        MDPConstants.UNEXPLORED -> {
                            canvas.drawRect(
                                (j * cellWidth).toFloat(), ((numberRows-1-i) * cellHeight).toFloat(),
                                ((j + 1) * cellWidth).toFloat(), ((numberRows - i) * cellHeight).toFloat(),
                                paintUnexplored
                            )
                        }
                    }
                }
                x++
            }
        }
    }

    // draw robot and its head based on the direction
    private fun drawRobot(canvas: Canvas){
        robot@ for (i in 0 until numberColumns) {
            for (j in 0 until numberRows) {
                if(robot.first == i && robot.second == j){
                    val left = (i * cellWidth).toFloat()
                    val top = ((numberRows-1-j) * cellHeight).toFloat()
                    val right = ((i + 1) * cellWidth).toFloat()
                    val bottom = ((numberRows - j) * cellHeight).toFloat()

                    val y = top + ((bottom - top)/2)
                    val x = left + ((right - left)/2)
                    val radius = ((cellWidth * 2) * 1.8 / 3).toFloat()
                    val robotHeadRadius = (radius * 0.3).toFloat()
                    canvas.drawCircle(x,y,radius,paintRobot)

                    // depending on the robot direction, we draw a circle with an offset
                    when(robotDirection){
                        "n" -> {
                            canvas.drawCircle(x,y-28,robotHeadRadius,paintRobotHead)
                        }
                        "e" -> {
                            canvas.drawCircle(x+28,y,robotHeadRadius,paintRobotHead)
                        }
                        "s" -> {
                            canvas.drawCircle(x,y+28,robotHeadRadius,paintRobotHead)
                        }
                        "w" -> {
                            canvas.drawCircle(x-28,y,robotHeadRadius,paintRobotHead)
                        }
                        else -> {
                            // set to north
                            canvas.drawCircle(x,y-28,robotHeadRadius,paintRobotHead)
                        }
                    }
                    break@robot
                }
            }
        }
    }

    private fun drawWaypoint(canvas: Canvas){
        waypoint@ for (i in 0 until numberColumns) {
            for (j in 0 until numberRows) {
                if(waypoint?.first == i && waypoint?.second == j && waypoint != null ){
                    canvas.drawRect(
                        (i * cellWidth).toFloat(), ((numberRows-1-j) * cellHeight).toFloat(),
                        ((i + 1) * cellWidth).toFloat(), ((numberRows - j) * cellHeight).toFloat(),
                        paintWaypoint
                    )
                    break@waypoint
                }
            }
        }
    }

    private fun drawDetectedImages(canvas: Canvas){
        for (i in 0 until numberColumns) {
            for (j in 0 until numberRows) {
                for (image in images){
                    // check x and y equals
                    if(image.first.first == i && image.first.second == j){
                        canvas.drawRect(
                            (i * cellWidth).toFloat(), ((numberRows-1-j) * cellHeight).toFloat(),
                            ((i + 1) * cellWidth).toFloat(), ((numberRows - j) * cellHeight).toFloat(),
                            paintObstacle
                        )
                        val left = (i * cellWidth).toFloat()
                        val top = ((numberRows-1-j) * cellHeight).toFloat()
                        val right = ((i + 1) * cellWidth).toFloat()
                        val bottom = ((numberRows - j) * cellHeight).toFloat()
                        val x = left + ((right - left)/2)
                        val y = top + ((bottom - top)/1.7)

                        Log.d("ArenaGridView","left: $left, top: $top, right: $right, bottom: $bottom, x: $x, y:$y")
                        val myTextPaint = TextPaint()
                        myTextPaint.setAntiAlias(true)
                        myTextPaint.setTextSize(8 * resources.displayMetrics.density)
                        myTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
                        val color = ContextCompat.getColor(context, R.color.colorOnPrimary)
                        myTextPaint.setColor(color)
                        myTextPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText(image.second.toString(),x,y.toFloat(),myTextPaint)
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)
        if(event?.action == MotionEvent.ACTION_DOWN){
            val column = (event.x / cellWidth).toInt() // x
            val row = MDPConstants.NUM_ROWS - 1 - (event.y / cellHeight).toInt() // y
            touchRectangle = Pair(column,row)
            invalidate()
            Log.d("ArenaGridView","column: $column row: $row")
            val intent = Intent(context, SelectCoordinateActivity::class.java)
            intent.putExtra("GRID_NUMBER",123)
            intent.putExtra("X",column.toString())
            intent.putExtra("Y",row.toString())
            (context as Activity).startActivityForResult(
                intent,
                ActivityConstants.REQUEST_COORDINATE
            )
        }
        return true
    }

    fun drawTouchEffect(canvas: Canvas){
        for (i in 0 until numberColumns) {
            for (j in 0 until numberRows) {
                if(touchRectangle?.first == i && touchRectangle?.second == j && touchRectangle != null){
                    canvas.drawRect(
                        (i * cellWidth).toFloat(),
                        ((numberRows - 1 - j) * cellHeight).toFloat(),
                        ((i + 1) * cellWidth).toFloat(),
                        ((numberRows - j) * cellHeight).toFloat(),
                        paintTouchEffect
                    )
                }
            }
        }
    }

    fun updateExploredAndObstacles(obstaclesArray: ArrayList<Char>){
        mapDescriptor.clear()
        mapDescriptor.addAll(obstaclesArray)
        if(autoUpdate){
            invalidate()
        }

    }

    fun setRobot(robotPair: Pair<Int,Int>, robotFacing: String){
        robot = robotPair
        robotDirection = robotFacing
        if(autoUpdate){
            Log.d("ArenaGridView","Updating new robot position")
            invalidate()
        }
    }

    fun addDetectedImages(image: Pair<Pair<Int,Int>,Int>){
        images.add(image)
        if(autoUpdate){
            Log.d("ArenaGridView","Updating images")
            invalidate()
        }
    }

    fun setWaypoint(waypointPair: Pair<Int,Int>){
        waypoint = waypointPair
        if(autoUpdate){
            Log.d("ArenaGridView","Setting new waypoint position")
            invalidate()
        }
    }

    fun setAutoUpdate(boolean: Boolean){
        autoUpdate = boolean
    }

    // remove the rectangle after you tap on one of the grid in the arena
    fun removeTouchEffect(){
        touchRectangle = null
        invalidate()
    }

    // clear waypoints, mapdescriptor, and images
    // resets robot position to the start zone pointing to north
    fun resetMap(){
        waypoint = null
        images.clear()
        robot = Pair(1,1)
        robotDirection = "n"
        mapDescriptor.clear()

        if(autoUpdate){
            Log.d("ArenaGridView","Resetting map")
            invalidate()
        }
    }

//    fun getPercentageExplored(): Int{
//        val percentage = ((exploredCount / 300) * 100)
//        Log.d("ArenaGridView","exploredCount: $exploredCount percentage: $percentage")
//        return ((exploredCount / 300) * 100)
//    }
}