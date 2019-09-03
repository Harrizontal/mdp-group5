package com.harrizontal.mdpgroup5.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.harrizontal.mdpgroup5.R

class GridAxisAdapter(private val mContext: Context, numCoordinates: Int) : BaseAdapter() {
    private val mItems: IntArray

    init {
        mItems = IntArray(numCoordinates)

        for (i in 0 until numCoordinates)
            mItems[i] = i
    }

    override fun getCount(): Int {
        return mItems.size
    }

    override fun getItem(index: Int): Any {
        return mItems[index]
    }

    override fun getItemId(i: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val gridView: View

        if(convertView == null){
            val inflater = mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


            gridView = inflater.inflate(R.layout.list_grid, null)

            gridView.findViewById<TextView>(R.id.text_color).text = position.toString()
        }else{
            gridView = convertView
        }

        return gridView
    }
}