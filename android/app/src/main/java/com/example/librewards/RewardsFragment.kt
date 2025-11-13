package com.example.librewards

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.librewards.models.Product
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_rewards.*
import kotlinx.android.synthetic.main.fragment_rewards.view.*
import kotlinx.android.synthetic.main.fragment_timer.*
import kotlinx.android.synthetic.main.popup_layout.*


class RewardsFragment : Fragment(), RecyclerAdapter.OnProductListener {
    private lateinit var popup: Dialog
    private lateinit var fh: FirebaseHandler
    private lateinit var mainActivity: MainActivity
    private lateinit var database: DatabaseReference
    private lateinit var productsList: MutableList<Product>
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>? = null
    private var listener: RewardsListener? = null
    private lateinit var v: View
    private var counter: Int? = null
    private lateinit var productPopup : Dialog

    //Interface that consists of a method that will update the points in "TimerFragment"
    interface RewardsListener {
        fun onPointsRewardsSent(points: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            counter = requireArguments().getInt("param2")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_rewards, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = activity as MainActivity
        layoutManager = LinearLayoutManager(context)
        "See rewards from ${mainActivity.university}".also { view.rewardsText.text = it }
        view.rewardsRecycler.layoutManager = layoutManager
        productsList = mutableListOf()
        adapter = RecyclerAdapter(requireActivity(), productsList, this)
        view.rewardsRecycler.adapter = adapter
        database = FirebaseDatabase.getInstance().reference
            .child("products")
            .child(mainActivity.university)
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productsList.clear()
                for (dataSnapshot in snapshot.children) {
                    val product = dataSnapshot.getValue(Product::class.java)
                    productsList.add(product!!)

                }
                (adapter as RecyclerAdapter).notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        fh = FirebaseHandler()
    }

    private fun calculatePointsFromPurchase(position: Int) {
        val pointsInt =
            Integer.parseInt(rewardsPoints.text.toString()) - Integer.parseInt(productsList[position].productcost!!)

        if (pointsInt > 0) {
            rewardsPoints.text = pointsInt.toString()
            minusPointsListener(Integer.parseInt(productsList[position].productcost!!))
        } else {
            toastMessage("You do not have sufficient points for this purchase")
        }

    }

    //Method that creates a custom popup
    private fun showTextPopup(text: String) {
        popup = Dialog(requireActivity())
        popup.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.setContentView(R.layout.popup_layout)
        popupText.text = text
        closeBtn.setOnClickListener { popup.dismiss() }
        popup.show()
    }

    //Method that creates a popup
    private fun showImagePopup(list: MutableList<Product>, position: Int) {
        val redeemRef = FirebaseDatabase.getInstance()
            .reference.child("users")
            .child(fh.hashFunction(mainActivity.email))
            .child("redeemingReward")
        redeemRef.setValue("0")
        productPopup = Dialog(requireActivity())
        productPopup.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        productPopup.setContentView(R.layout.popup_layout)
        val popUpImageView = productPopup.findViewById<ImageView>(R.id.popupImageView)
        val popUpCost = productPopup.findViewById<TextView>(R.id.popupCost)
        val popUpText = productPopup.findViewById<TextView>(R.id.popupText)
        val closeBtn = productPopup.findViewById<AppCompatImageView>(R.id.closeBtn)
        popUpImageView.layoutParams.height = 150
        popUpImageView.layoutParams.width = 150
        popUpImageView.updateLayoutParams<ViewGroup.MarginLayoutParams> { setMargins(30, 0, 0, 0) }
        val popupQr = productPopup.findViewById<ImageView>(R.id.popupQr)
        val drawableQR = mainActivity.qrCode.drawable
        popupQr.setImageDrawable(drawableQR)
        popupQr.updateLayoutParams<ViewGroup.MarginLayoutParams> { setMargins(50) }
        popUpText.text = list[position].productname
        "${list[position].productcost} points".also { popUpCost.text = it }
        popUpCost.textSize = 20F
        popUpText.textSize = 25F
        popUpCost.updateLayoutParams<ViewGroup.MarginLayoutParams> { setMargins(0, 0, 0, 50) }
        Picasso.get().load(list[position].productimage).into(popUpImageView)

        var redeemed: String

            val redeemListener = redeemRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    redeemed = snapshot.value.toString()
                    if (redeemed == "1") {
                        redeemRef.setValue("0")
                        calculatePointsFromPurchase(position)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Could not access database")
                }

            })

        closeBtn.setOnClickListener {
            productPopup.dismiss()
        }
        productPopup.setOnDismissListener { redeemRef.removeEventListener(redeemListener) }
        productPopup.show()

    }

    //Creating a preference for activity on first start-up only
    private fun firstStart(): Boolean {
        val rewardsPrefs = mainActivity.getSharedPreferences("rewardsPrefs", Context.MODE_PRIVATE)
        return rewardsPrefs.getBoolean("firstStart", true)
    }

    //Method creating a custom Toast message
    private fun toastMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun minusPointsListener(minusValue: Int) {
        val refChild = fh.getChild("users", mainActivity.email, "points")
        var dbPoints: String
        val pointsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dbPoints = dataSnapshot.value.toString()
                val finalPoints = Integer.parseInt(dbPoints) - minusValue
                refChild.setValue(finalPoints.toString())
                rewardsPoints.text = finalPoints.toString()
                listener?.onPointsRewardsSent(finalPoints)
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        }
        refChild.addListenerForSingleValueEvent(pointsListener)
    }

    //Method that is used between fragments to update each other's points
    fun updatedPoints(newPoints: Int) {
        v.rewardsPoints.text = newPoints.toString()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is RewardsListener) {
            context
        } else {
            throw RuntimeException(context.toString() + "must implement TimerListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        val TAG: String = RewardsFragment::class.java.simpleName
    }

    override fun onProductClick(position: Int) {
        showImagePopup(productsList, position)
    }

}