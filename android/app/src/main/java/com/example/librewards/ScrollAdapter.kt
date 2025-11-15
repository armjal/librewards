package com.example.librewards

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.librewards.models.Product
import com.squareup.picasso.Picasso

class ScrollAdapter(
    var context: Context,
    var list: MutableList<Product>,
    var mOnProductListener: RecyclerAdapter.OnProductListener
) : RecyclerView.Adapter<ScrollAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.reward_card, parent, false)
        return ViewHolder(v, mOnProductListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product: Product = list[position]
        holder.itemTitle.text = product.productname
        (product.productcost + " points").also { holder.itemDetail.text = it }
        Picasso.get().load(product.productimage).into(holder.itemImage)

    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(
        itemView: View,
        var onProductListener: RecyclerAdapter.OnProductListener
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var itemImage: ImageView = itemView.findViewById(R.id.homeProductImage)
        var itemTitle: TextView = itemView.findViewById(R.id.homeProductTitle)
        var itemDetail: TextView = itemView.findViewById(R.id.homeProductCost)

        init {

            itemView.setOnClickListener(this)

        }

        override fun onClick(v: View?) {
            onProductListener.onProductClick(adapterPosition)
        }
    }

}
