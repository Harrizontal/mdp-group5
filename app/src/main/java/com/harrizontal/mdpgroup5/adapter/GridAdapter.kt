package com.harrizontal.mdpgroup5.adapter

import android.content.Context
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat.getSystemService
import android.view.LayoutInflater
import com.harrizontal.mdpgroup5.R


class GridAdapter(
    private val context: Context,
    private val books: ArrayList<String>,
    private val rows: Int,
    private val column: Int
) : BaseAdapter() {

    // 2
    override fun getCount(): Int {
        return rows * column
    }

    // 3
    override fun getItemId(position: Int): Long {
        return 0
    }

    // 4
    override fun getItem(position: Int): Any? {
        return null
    }

    // 5
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val gridView: View

        if(convertView == null){
            val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


            gridView = inflater.inflate(R.layout.list_grid, null)

            gridView.findViewById<TextView>(R.id.text_color).text = position.toString()
        }else{
            gridView = convertView
        }

        return gridView
    }

}