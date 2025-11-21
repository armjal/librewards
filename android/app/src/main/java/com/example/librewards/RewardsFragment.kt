package com.example.librewards

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.librewards.databinding.FragmentRewardsBinding
import com.example.librewards.databinding.PopupLayoutBinding
import com.example.librewards.models.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import androidx.core.graphics.drawable.toDrawable


class RewardsFragment : Fragment(), RecyclerAdapter.OnProductListener {
    private lateinit var fh: FirebaseHandler
    private lateinit var mainActivity: MainActivity
    private lateinit var database: DatabaseReference
    private lateinit var productsList: MutableList<Product>
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>? = null
    private var listener: RewardsListener? = null
    private var counter: Int? = null
    private lateinit var productPopup: Dialog

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = activity as MainActivity
        layoutManager = LinearLayoutManager(context)
        "See rewards from ${mainActivity.university}".also { binding.rewardsText.text = it }
        binding.rewardsRecycler.layoutManager = layoutManager
        productsList = mutableListOf()
        adapter = RecyclerAdapter(productsList, this)
        binding.rewardsRecycler.adapter = adapter
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun calculatePointsFromPurchase(position: Int) {
        val pointsInt =
            Integer.parseInt(binding.rewardsPoints.text.toString()) - Integer.parseInt(productsList[position].productCost!!)

        if (pointsInt > 0) {
            binding.rewardsPoints.text = pointsInt.toString()
            minusPointsListener(Integer.parseInt(productsList[position].productCost!!))
        } else {
            toastMessage("You do not have sufficient points for this purchase")
        }

    }

    private fun showImagePopup(list: MutableList<Product>, position: Int) {
        val redeemRef = FirebaseDatabase.getInstance()
            .reference.child("users")
            .child(hashFunction(mainActivity.email))
            .child("redeemingReward")
        redeemRef.setValue("0")
        productPopup = Dialog(requireActivity())
        productPopup.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val popupBinding = PopupLayoutBinding.inflate(layoutInflater)
        productPopup.setContentView(popupBinding.root)

        popupBinding.popupImageView.layoutParams.height = 150
        popupBinding.popupImageView.layoutParams.width = 150
        popupBinding.popupImageView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(
                30,
                0,
                0,
                0
            )
        }

//         val drawableQR = mainActivity.qrCode.drawable
//         popupBinding.popupQr.setImageDrawable(drawableQR)

        popupBinding.popupQr.updateLayoutParams<ViewGroup.MarginLayoutParams> { setMargins(50) }
        popupBinding.popupText.text = list[position].productName
        "${list[position].productCost} points".also { popupBinding.popupCost.text = it }
        popupBinding.popupCost.textSize = 20F
        popupBinding.popupText.textSize = 25F
        popupBinding.popupCost.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(
                0,
                0,
                0,
                50
            )
        }
        Picasso.get().load(list[position].productImageUrl).into(popupBinding.popupImageView)

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

        popupBinding.closeBtn.setOnClickListener {
            productPopup.dismiss()
        }
        productPopup.setOnDismissListener { redeemRef.removeEventListener(redeemListener) }
        productPopup.show()

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
                binding.rewardsPoints.text = finalPoints.toString()
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
        binding.rewardsPoints.text = newPoints.toString()
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
