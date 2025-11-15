package com.example.librewards

import android.content.Context
import java.util.*

class ListFromFile     //Parameter of 'Context' to state to the list which activity will be using the class
(private val context: Context) {
    //Method to read each line of a text file that is being read and assign the lines in the file to a list
    fun readLine(path: String?): MutableList<String> {
        val lines: MutableList<String>
        val inputStream: String = context.assets.open(path!!).bufferedReader().use { it.readText() }
        lines = inputStream.split("\n").toMutableList()
        //Returns the list of values
        return lines
    }
}