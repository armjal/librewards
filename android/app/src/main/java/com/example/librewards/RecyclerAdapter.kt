package com.example.librewards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.librewards.models.Product
import com.squareup.picasso.Picasso

class RecyclerAdapter(
    var list: MutableList<Product>,
    var mOnProductListener: OnProductListener
) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.card_layout, parent, false)
        return ViewHolder(v, mOnProductListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product: Product = list[position]
        holder.itemTitle.text = product.productName
        (product.productCost + " points").also { holder.itemDetail.text = it }
        Picasso.get().load(product.productImageUrl).into(holder.itemImage)

    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(itemView: View, var onProductListener: OnProductListener) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var itemImage: ImageView = itemView.findViewById(R.id.rewardImage)
        var itemTitle: TextView = itemView.findViewById(R.id.rewardTitle)
        var itemDetail: TextView = itemView.findViewById(R.id.rewardCost)

        init {

            itemView.setOnClickListener(this)

        }

        override fun onClick(v: View?) {
            if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                onProductListener.onProductClick(bindingAdapterPosition)
            }
        }
    }

    fun interface OnProductListener {
        fun onProductClick(position: Int)
    }

}
