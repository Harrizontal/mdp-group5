package com.harrizontal.mdpgroup5.constants

interface SharedPreferenceConstants {
    companion object{
        val SHARED_PREF_MDP = "SHARED_PREF_MDP"
        val SHARED_PREF_FUNCTION_1 = "SHARED_PREF_FUNCTION_1"
        val SHARED_PREF_FUNCTION_2 = "SHARED_PREF_FUNCTION_2"
        val SHARED_PREF_MAP_UPDATE = "SHARED_PREF_MAP_UPDATE" // not needed to be a shared pref in the project scope

        // default values for shared preference
        val DEFAULT_VALUE_MAP_UPDATE = true
        val DEFAULT_VALUE_FUNCTION_1 = "Hello"
        val DEFAULT_VALUE_FUNCTION_2 = "Hello2"
    }
}