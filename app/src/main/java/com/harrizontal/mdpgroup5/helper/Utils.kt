package com.harrizontal.mdpgroup5.helper

class Utils {
    fun getMapDescriptor(mapDescriptorString: String): ArrayList<Char> {
        val mapDescriptor = ArrayList<Char>()

        for (i in 0 until mapDescriptorString.length)
            mapDescriptor.add(mapDescriptorString[i])

        return mapDescriptor
    }
}