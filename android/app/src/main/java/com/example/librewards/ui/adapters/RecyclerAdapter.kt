package com.example.librewards.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.librewards.R
import com.example.librewards.data.models.Product
import com.example.librewards.data.models.ProductEntry
import com.squareup.picasso.Picasso

class RecyclerAdapter(
    var list: MutableList<ProductEntry>,
    var mOnProductListener: OnProductListener,
) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.card_layout, parent, false)
        return ViewHolder(v, mOnProductListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product: Product = list[position].product
        holder.itemTitle.text = product.productName
        (product.productCost + " points").also { holder.itemDetail.text = it }
        Picasso.get().load(product.productImageUrl).into(holder.itemImage)
    }

    fun updateList(newList: List<ProductEntry>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = list.size

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
