package com.harrizontal.mdpgroup5.helper

import android.util.Log
import com.harrizontal.mdpgroup5.constants.MDPConstants
import kotlin.math.round
import kotlin.math.roundToInt


class Utils {

    fun getcombinedMapDescriptor(mapDescriptorString: String): ArrayList<Char>{
        val testingDescriptor = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff;001000200040070000000038001000000004000820107e300400000000610080200040008000"
        val testingDescriptor2 = "fffffffffffefffcfffbfff7bfef7fc0ff81ff003e007c007800f000e0008000000000000003;01c0000000000000000fc40020010000210002"

        // cao liu's descriptors testing.
        val testingDescriptor3 = "fffb7ff6fffc7ffcfffbbff63fcc7e007c003800700000000000000000000000000000000003;002001000c0001000c0001800020"
        val testingDescriptor4 = "fffffffffffe7ffcfffbbff6ffedffc3ff81ff007e00fc01f803f03fe07fc0ff80ff01fe00ff;0030006000c00008006000038000000010420800001004040000"
        val testingDescriptor5 = "fffffffffffe7ffcfffbbff6ffedffc3ff81ff007e00fc01f803f007e001c003000600000003;0030006000c0000800600003800000001042080020"
        val testingDescriptor6 = "fffffffffffe7ffcfffbfff7ffefffffffffffffffffffffffffffffffffffffffffffffffff;0030006000c0000800700000e000000080010043c080020004000004008800010002000600"

        val testingDescriptor7 = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff;00000000000003800000383800100000000400080010fe3004000000006000c0100020004000"

        val testingDescriptor8 = "ff007e00fc004000800300040000000000000000000000000000000000000000000000000003;000002\n".trim()

        val trimmedMapDescriptor = mapDescriptorString.trim()
        val mapDescriptor = trimmedMapDescriptor.split(";")

        val firstDescriptor = mapDescriptor[0]
        val secondDescriptor = mapDescriptor[1]

        Log.d("Utils","firstDescriptor: $firstDescriptor")
        Log.d("Utils","secondDescriptor: $secondDescriptor")
        val binaryFirstDescriptor = hexToBinary(firstDescriptor).drop(2).dropLast(2)
        //Log.d("Utils","binaryFirstDescriptor: $binaryFirstDescriptor")
        val binarySecondDescriptor = hexToBinary(secondDescriptor)
        //Log.d("Utils","binarySecondDescriptor: $binarySecondDescriptor")

        val mapDescriptor2 = ArrayList<Char>()

        var part2counter = 0
        var binary: Char
        for (i in 0 until binaryFirstDescriptor.length){
            binary = binaryFirstDescriptor[i]
            if(binary == '1' && part2counter < binarySecondDescriptor.length){
                if(binarySecondDescriptor[part2counter] == '1'){
                    // means is obstacle
                    mapDescriptor2.add(MDPConstants.OBSTACLE)
                }else{
                    // means is explored
                    mapDescriptor2.add(MDPConstants.EXPLORED)
                }
                part2counter++
            }else{
                mapDescriptor2.add(MDPConstants.UNEXPLORED)
            }
        }

        //Log.d("Utils","mapDescriptor2: ${mapDescriptor2}")
        return mapDescriptor2
    }

    private fun hexToBinary(s: String): String {
        val sb = StringBuilder()
        val paddings = "0000"
        var i = 0
        while (i < s.length) {
            //Log.d("Utils","s[i]: "+s[i])
            val hex = Integer.parseInt(s[i].toString(), 16)
            //Log.d("Utils","hex: $hex")
            val binary = Integer.toBinaryString(hex)
            //Log.d("Utils","binary: $binary")
            val padding = paddings.substring(0, paddings.length - binary.length)
            sb.append(padding)
            sb.append(binary)
            i += 1
        }

        return sb.toString()
    }


    fun recalculateMiddleRobotPosition(xCoordinate: Int, yCoordinate: Int): Pair<Int,Int>{

        var newXValue: Int
        var newYValue: Int

        when(xCoordinate){
            0 -> {
                newXValue = 1
            }
            (MDPConstants.NUM_COLUMNS - 1) -> {
                newXValue = MDPConstants.NUM_COLUMNS - 2
            }
            else ->{
                newXValue = xCoordinate
            }
        }

        when(yCoordinate){
            0 -> {
                newYValue = 1
            }
            (MDPConstants.NUM_ROWS - 1) -> {
                newYValue = MDPConstants.NUM_ROWS - 2
            }
            else -> {
                newYValue = yCoordinate
            }
        }

        return Pair(newXValue,newYValue)
    }


    fun countExploredPercentage(mapDescriptor: ArrayList<Char>): Int{
        var exploredCount: Double = 0.00
        for (item in mapDescriptor){
            if(item != MDPConstants.UNEXPLORED){
                exploredCount++
            }
        }
        val percentage = (exploredCount/ (MDPConstants.NUM_ROWS * MDPConstants.NUM_COLUMNS))* 100
        //Log.d("Utils","exploredCount: $exploredCount percentage explored: $percentage")
        return percentage.roundToInt()
    }

    fun displayMovement(movement: String): String{

        when(movement) {
            "forward" -> return "Forwarding"
            "right" -> return "Rotating Right"
            "left" -> return "Rotating Left"
            "I" -> return "Right Wall Calibrating"
            "G" -> return "Top Right Way Calibrating"
            else ->
                return "Moving"
        }
    }
}