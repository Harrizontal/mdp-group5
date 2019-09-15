package com.harrizontal.mdpgroup5.constants

interface MDPConstants {

    companion object{
        val NUM_ROWS: Int = 20
        val NUM_COLUMNS: Int = 15
        val DEFAULT_MAP_DESCRIPTOR_STRING =
            "0000000000000000000000000000000000000000000000000000000000000000000000000000" // map descriptor part 1 but all unexplored

        val DEFAULT_START_AREA = arrayListOf<Int>(255,256,257,270,271,272,285,286,287) // these numbers are id in the recycleview
        val DEFAULT_END_AREA = arrayListOf<Int>(12,13,14,27,28,29,42,43,44)
        val DEFAULT_ROBOT_POSITION = arrayListOf(
            Pair(255,Pair(MDPConstants.ROBOT_TOP_LEFT,false)),
            Pair(256,Pair(MDPConstants.ROBOT_TOP,true)),
            Pair(257,Pair(MDPConstants.ROBOT_TOP_RIGHT,false)),
            Pair(270,Pair(MDPConstants.ROBOT_MIDDLE_LEFT,false)),
            Pair(271,Pair(MDPConstants.ROBOT_MIDDLE,false)),
            Pair(272,Pair(MDPConstants.ROBOT_MIDDLE_RIGHT,false)),
            Pair(285,Pair(MDPConstants.ROBOT_BOTTOM_LEFT,false)),
            Pair(286,Pair(MDPConstants.ROBOT_BOTTOM,false)),
            Pair(287,Pair(MDPConstants.ROBOT_BOTTOM_RIGHT,false))
        ) // robot is at the starting position

        val UNEXPLORED = '0'
        val EXPLORED = '1'
        val OBSTACLE = '2'
        val START_AREA = '3'
        val END_AREA = '4'
        val ROBOT_HEAD = '5'
        val ROBOT_BODY = '6'

        val ROBOT_TOP_LEFT = '1'
        val ROBOT_TOP = '2'
        val ROBOT_TOP_RIGHT = '3'
        val ROBOT_MIDDLE_LEFT = '4'
        val ROBOT_MIDDLE = '5'
        val ROBOT_MIDDLE_RIGHT = '6'
        val ROBOT_BOTTOM_LEFT = '7'
        val ROBOT_BOTTOM = '8'
        val ROBOT_BOTTOM_RIGHT = '9'

        // add more constants
    }
}