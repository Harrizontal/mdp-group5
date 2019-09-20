package com.harrizontal.mdpgroup5.helper

import android.util.Log
import com.harrizontal.mdpgroup5.constants.MDPConstants
import java.lang.Double.parseDouble
import java.math.BigInteger


class Utils {
    /**
     * Convert MDP CZ3004 Map descriptor format (part 1 and part2) into a format for RecycleView 2D Arena Map
     *
     */
    fun getMapDescriptorsToMapRecycleFormat(mapDescriptorString: String): ArrayList<Char> {
        //val message = mapDescriptorString.toByte()

        val testingDescriptor = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff;001000200040070000000038001000000004000820107e300400000000610080200040008000"
        val testingDescriptor2 = "fffffffffffefffcfffbfff7bfef7fc0ff81ff003e007c007800f000e0008000000000000003;01c0000000000000000fc40020010000210002";

        val descriptorParts = mapDescriptorString.split(";")

        val descriptorPartOne = descriptorParts[0]
        val descriptorPartTwo = descriptorParts[1]

        // error checking - just in case... why 76 because 304 / 4
        if(descriptorPartOne.length != 76){
            Log.e("Utils","MapDescriptor for the first part is not 76 length in terms of hex - Please check with algorithm side")
        }

        // converting hex string to binary for both descriptor
        // for first descriptor
        val binariesPartOne = BigInteger(descriptorPartOne,16).toString(2)
        val formatPad1 = "%" + descriptorPartTwo.length * 4 + "s"
        val formattedBinariesPart11 = String.format(formatPad1, binariesPartOne).replace(" ", "0") // pad leading 0s in front
        val removeFirstTwoBinary = formattedBinariesPart11.drop(2) // remove first 2 binary
        val formattedBinariesPartOne = removeFirstTwoBinary.dropLast(2) // remove last 2 binary

        // for second descriptor
        val binariesPartTwo = BigInteger(descriptorPartTwo,16).toString(2)
        val formatPad = "%" + descriptorPartTwo.length * 4 + "s"
        val formattedBinariesPartTwo = String.format(formatPad, binariesPartTwo).replace(" ", "0") // pad leading 0s in front
        // e.g when converting 2 Hex into binary, it will give 10. It should give 0010 after the above codes

        Log.d("Utils","formattedBinariesPartOne: ${formattedBinariesPartOne.length}")
        Log.d("Utils","formattedBinariesPartTwo: ${formattedBinariesPartTwo}")

        // combine part 1 and part 2 descriptors into a map descriptor with obstacle, unexplored and explored
        val mapDescriptor2 = ArrayList<Char>()
        var part2counter = 0
        var binary: Char
        for (i in 0 until formattedBinariesPartOne.length){
            binary = formattedBinariesPartOne[i]
            if(binary == '1' && part2counter < formattedBinariesPartTwo.length){
                if(formattedBinariesPartTwo[part2counter] == '1'){
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

        return mapDescriptor
    }


    /**
     * Convert HexString to BinaryString with the option to remove padding (first 2 and last 2 binaries)
     * @param hex (typically following map descriptor - 76 characters long
     * @param removePadding
     */
    private fun convertHexStringToBinaryString(hex: String,removePadding: Boolean): String{
        val binaries = BigInteger(hex,16).toString(2)
        val formatPad = "%" + hex.length * 4 + "s"
        val leadingBinaries = String.format(formatPad, binaries).replace(" ", "0")

        if(removePadding){
            val removeFirstTwoBinary = leadingBinaries.drop(2) // remove first 2 binary
            val formattedBinaries = removeFirstTwoBinary.dropLast(2) // remove last 2 binary
            return formattedBinaries
        }else{
            return leadingBinaries
        }
    }

    /**
     * Convert 76 Hex string into binary for map descriptor
     *
     */
    fun convertMapDescriptor1ToMapRecycleFormat(mapDescriptorString: String): ArrayList<Char>{
        val binaries = convertHexStringToBinaryString(mapDescriptorString,true)
        val test = ArrayList<Char>()
        Log.d("Utls","binaries: ${binaries.length}")
        for (x in 0 until binaries.length){
            test.add(binaries[x])
        }
        return test
    }
    /**
     * Return a arraylist of grid id (recycleview item id) of the robot body and robot head
     * @param robotMainPosition e.g 07,02,e
     */
    fun getRobotPositions(robotMainPosition: String): ArrayList<Pair<Int,Pair<Char,Boolean>>>{
        val parts = robotMainPosition.split(",")
        val robotHeadAndBody = ArrayList<Pair<Int,Pair<Char,Boolean>>>() // stores the grid id of the calculated robot and the type (robot head/robot body)

        // 3x3 robot size
        // [body1][body2 - n][body3]
        // [body4 - w][body5][body6 - e]
        // [body7][body8 - s][body9]
        // n,w,e,s is the robot direction
        // each body have a x and y

        // calculate the 2nd layer of robot first
        val x5 = parts[0].toInt()
        val y5 = parts[1].toInt()
        val x4 = x5 - 1
        val y4 = y5
        val x6 = x5 + 1
        val y6 = y5

        // calculate the 1st layer of robot
        val x1 = x4
        val y1 = y4 + 1
        val x2 = x5
        val y2 = y5 + 1
        val x3 = x6
        val y3 = y6 + 1

        // calculate the 3rd layer of robot
        val x7 = x4
        val y7 = y4 - 1
        val x8 = x5
        val y8 = y5 - 1
        val x9 = x6
        val y9 = y6 - 1

        // inefficient :(
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x1,y1),Pair(MDPConstants.ROBOT_TOP_LEFT,calculateRobotType(1,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x2,y2),Pair(MDPConstants.ROBOT_TOP,calculateRobotType(2,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x3,y3),Pair(MDPConstants.ROBOT_TOP_RIGHT,calculateRobotType(3,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x4,y4),Pair(MDPConstants.ROBOT_MIDDLE_LEFT,calculateRobotType(4,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x5,y5),Pair(MDPConstants.ROBOT_MIDDLE,calculateRobotType(5,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x6,y6),Pair(MDPConstants.ROBOT_MIDDLE_RIGHT,calculateRobotType(6,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x7,y7),Pair(MDPConstants.ROBOT_BOTTOM_LEFT,calculateRobotType(7,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x8,y8),Pair(MDPConstants.ROBOT_BOTTOM,calculateRobotType(8,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x9,y9),Pair(MDPConstants.ROBOT_BOTTOM_RIGHT,calculateRobotType(9,parts[2]))))

        return robotHeadAndBody
    }

    fun getImagePosition(imageCoordinateString: String): Pair<Int,Int>?{
        val parts = imageCoordinateString.split(",")

        val imageCoordinate = Utils().convertCoordinatesToGridId(parts[0].toInt(),parts[1].toInt())

        try {
            val num = parseDouble(parts[2])
        } catch (e: NumberFormatException) {
            return null
        }


        if (parts[2].toInt() == -1){
            // means no image detected at all.
            return null
        }else{
            return Pair(imageCoordinate,parts[2].toInt())
        }

    }


    // similiar function as top - just to clear checklist
    fun getRobotPositions2(robotMainPosition: String): ArrayList<Pair<Int,Pair<Char,Boolean>>>{
        val parts = robotMainPosition.split(",")
        val robotHeadAndBody = ArrayList<Pair<Int,Pair<Char,Boolean>>>() // stores the grid id of the calculated robot and the type (robot head/robot body)

        // 3x3 robot size
        // [body1][body2 - n][body3]
        // [body4 - w][body5][body6 - e]
        // [body7][body8 - s][body9]
        // n,w,e,s is the robot direction
        // each body have a x and y

        // calculate the 1st layer of the robot
        val x1 = parts[0].toInt()
        val y1 = parts[1].toInt()
        val x2 = x1 + 1
        val y2 = y1
        val x3 = x2 + 1
        val y3 = y2

        // calculate the 1st layer of robot
        val x4 = x1
        val y4 = y1 - 1
        val x5 = x2
        val y5 = y2 - 1
        val x6 = x3
        val y6 = y3 - 1

        // calculate the 3rd layer of robot
        val x7 = x4
        val y7 = y4 - 1
        val x8 = x5
        val y8 = y5 - 1
        val x9 = x6
        val y9 = y6 - 1

        // inefficient :(
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x1,y1),Pair(MDPConstants.ROBOT_TOP_LEFT,calculateRobotType(1,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x2,y2),Pair(MDPConstants.ROBOT_TOP,calculateRobotType(2,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x3,y3),Pair(MDPConstants.ROBOT_TOP_RIGHT,calculateRobotType(3,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x4,y4),Pair(MDPConstants.ROBOT_MIDDLE_LEFT,calculateRobotType(4,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x5,y5),Pair(MDPConstants.ROBOT_MIDDLE,calculateRobotType(5,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x6,y6),Pair(MDPConstants.ROBOT_MIDDLE_RIGHT,calculateRobotType(6,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x7,y7),Pair(MDPConstants.ROBOT_BOTTOM_LEFT,calculateRobotType(7,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x8,y8),Pair(MDPConstants.ROBOT_BOTTOM,calculateRobotType(8,parts[2]))))
        robotHeadAndBody.add(Pair(convertCoordinatesToGridId(x9,y9),Pair(MDPConstants.ROBOT_BOTTOM_RIGHT,calculateRobotType(9,parts[2]))))

        return robotHeadAndBody
    }

    // for checklist
    fun flipCoordinatesForADM(xCoordinate: Int, yCoordinate: Int): Pair<Int,Int>{
        val newYvalue = MDPConstants.NUM_ROWS - yCoordinate - 1
        return Pair(xCoordinate,newYvalue)
    }

    // for checklist
    fun getDirection(degree: Int): String{
        when(degree){
            in 0..89 -> {
                return "n"
            }
            in 90..179 -> {
                return "e"
            }
            in 180..269 -> {
                return "s"
            }
            in 270..359 ->{
                return "w"
            }
            else -> {
                return "n"
            }
        }
    }



    fun convertCoordinatesToGridId(coordinateX:Int, coordinateY: Int): Int{

        // calculate row based on y
        var x = coordinateX
        var y = coordinateY
        if(coordinateX < 0){
            x = 0
            Log.e("Utils","X coordinate is less than 0")
        }

        if(coordinateY < 0){
            y = 0
            Log.e("Utils","Y coordinate is less than 0")
        }

        val row = MDPConstants.NUM_ROWS - y - 1
        val id = (row * MDPConstants.NUM_COLUMNS) + x

        // if x is 2, y is 19
        // x is 2, then 2 lor
        // y is 19, then 0
        // y is 18, then 15
        // y is 17, then 30

        return id
    }

    private fun calculateRobotType(bodyPosition: Int, direction: String): Boolean{
        when(bodyPosition){
            2 -> {
                if(direction == "n"){
                    return true
                }
            }
            4 -> {
                if(direction == "w"){
                    return true
                }
            }
            6 -> {
                if(direction == "e") {
                    return true
                }
            }
            8 -> {
                if(direction == "s"){
                    return true
                }
            }
        }
        return false
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

    fun getMapDescriptorsToMapRecycleFormat2(mapDescriptorString: String): ArrayList<Char> {

        Log.d("Utils","mapDescriptorString's length: ${mapDescriptorString.length}")
        val binary = convertHexStringToBinaryString(mapDescriptorString,false)

        Log.d("Utils","binary: ${binary.length}")
        val mapDescriptor = ArrayList<Char>()
        for (i in 0 until binary.length){
            if(binary[i].equals('1')){
                mapDescriptor.add('2')
            }else{
                mapDescriptor.add(binary[i])
            }

        }

        return mapDescriptor

    }

}