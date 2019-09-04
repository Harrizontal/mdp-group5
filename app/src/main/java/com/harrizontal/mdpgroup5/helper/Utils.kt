package com.harrizontal.mdpgroup5.helper

import android.util.Log

class Utils {
    fun getMapDescriptor(mapDescriptorString: String): ArrayList<Char> {
        val mapDescriptor = ArrayList<Char>()

        for (i in 0 until mapDescriptorString.length){
            mapDescriptor.add(mapDescriptorString[i])
            Log.d("Utils","Adding ${mapDescriptorString[i]} to mapDescriptor arraylist")
        }
        return mapDescriptor
    }
}