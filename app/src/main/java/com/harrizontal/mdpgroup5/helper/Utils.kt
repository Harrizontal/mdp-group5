package com.harrizontal.mdpgroup5.helper

import android.util.Log
import com.harrizontal.mdpgroup5.constants.MDPConstants
import java.math.BigInteger
import android.R.string



class Utils {
    fun getMapDescriptor(mapDescriptorString: String): ArrayList<Char> {
        //val message = mapDescriptorString.toByte()

        val testingDescriptor = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff;001000200040070000000038001000000004000820107e300400000000610080200040008000"

        val descriptorParts = testingDescriptor.split(";")

        val descriptorPartOne = descriptorParts[0]
        val descriptorPartTwo = descriptorParts[1]

        // error checking - just in case... why 76 because 304 / 4
        if(descriptorPartOne.length != 76){
            Log.e("Utils","MapDescriptor for the first part is not 76 length in terms of hex - Please check with algorithm side")
        }

        // converting hex string to binary for both descriptor
        // for first descriptor
        val binariesPartOne = BigInteger(descriptorPartOne,16).toString(2)
        val removeFirstTwoBinary = binariesPartOne.drop(2) // remove first 2 binary
        val formattedBinariesPartOne = removeFirstTwoBinary.dropLast(2) // remove last 2 binary

        // for second descriptor
        val binariesPartTwo = BigInteger(descriptorPartTwo,16).toString(2)
        val formatPad = "%" + descriptorPartTwo.length * 4 + "s"
        val formattedBinariesPartTwo = String.format(formatPad, binariesPartTwo).replace(" ", "0") // pad leading 0s in front
        // e.g when converting 2 Hex into binary, it will give 10. It should give 0010 after the above codes

        Log.d("Utils","formattedBinariesPartOne: ${formattedBinariesPartOne}")
        Log.d("Utils","formattedBinariesPartTwo: ${formattedBinariesPartTwo}")

        // combine part 1 and part 2 descriptors into a map descriptor with obstacle, unexplored and explored
        val mapDescriptor2 = ArrayList<Int>()
        var part2counter = 0
        var binary: Char
        for (i in 0 until formattedBinariesPartOne.length){
            binary = formattedBinariesPartOne[i]
            if(binary == '1' && part2counter < formattedBinariesPartTwo.length){
                if(formattedBinariesPartTwo[part2counter] == '1'){
                    // means is obstacle
                    mapDescriptor2.add(2)
                }else{
                    // means is explored
                    mapDescriptor2.add(1)
                }
                part2counter++

            }else{
                mapDescriptor2.add(0)
            }
        }

        // convert ArrayList<Int> into string, and remove command, brackets and spaces
        val unarrangedBinary = mapDescriptor2.toString()
            .replace(",","")
            .replace("[","")
            .replace("]","")
            .replace(" ","")


        // first 10 of binary is meant for first row. We split in 15 columns
        val chunckedBinaryForMap2 = unarrangedBinary.chunked(MDPConstants.NUM_COLUMNS)

        // and reverse it, convert to string - this is important.
        val reversedBinaryForMap2 = chunckedBinaryForMap2.reversed()

        // and remove the commas,spaces and square brackets
        val correctedBinaryForMap2 = reversedBinaryForMap2
            .toString()
            .replace(",","")
            .replace("[","")
            .replace("]","")
            .replace(" ","")


        Log.d("Utils","mapDescriptor2: ${correctedBinaryForMap2}")


        val mapDescriptor = ArrayList<Char>()
        for (i in 0 until correctedBinaryForMap2.length){
            mapDescriptor.add(correctedBinaryForMap2[i])
        }

        convertCoordinatesToGridId(0,18);
        return mapDescriptor
    }

    /**
     * Return a arraylist of grid id (recycleview item id) of the robot body and robot head
     * @param robotMainPosition e.g 07,02,e
     */
    fun getRobotPositions(robotMainPosition: String): Int{

        val parts = robotMainPosition.split(",")
        return convertCoordinatesToGridId(parts[0].toInt(),parts[1].toInt())
    }

    fun convertCoordinatesToGridId(coordinateX:Int, coordinateY: Int): Int{

        // calculate row based on y
        val row = MDPConstants.NUM_ROWS - coordinateY - 1

        val id = (row * MDPConstants.NUM_COLUMNS) + coordinateX

        // if x is 2, y is 19
        // x is 2, then 2 lor
        // y is 19, then 0
        // y is 18, then 15
        // y is 17, then 30

        Log.d("Utils","Coordinate: "+id)
        return id
    }

}