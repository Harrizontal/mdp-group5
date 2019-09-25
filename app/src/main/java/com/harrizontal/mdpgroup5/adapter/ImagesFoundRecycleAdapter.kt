package com.harrizontal.mdpgroup5.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.harrizontal.mdpgroup5.R
import kotlinx.android.synthetic.main.list_item_text.view.*

class ImagesFoundRecycleAdapter(
    private val imagesFound: ArrayList<String>,
    private val context: Context
): RecyclerView.Adapter<ImagesFoundRecycleAdapter.ImageFoundHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageFoundHolder {
        return ImageFoundHolder(LayoutInflater.from(context).inflate(R.layout.list_item_text, parent, false))
    }

    override fun getItemCount(): Int {
        return imagesFound.size
    }

    override fun onBindViewHolder(holder: ImageFoundHolder, position: Int) {
        val imageString = imagesFound.get(position)
        holder.itemView.row_item.text = imageString
    }


    class ImageFoundHolder(v: View) : RecyclerView.ViewHolder(v){
        val rowItem = v.row_item
    }
}