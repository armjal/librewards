package com.example.librewards

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.librewards.databinding.FragmentRewardsBinding
import com.example.librewards.databinding.PopupLayoutBinding
import com.example.librewards.models.Product
import com.example.librewards.models.ProductEntry
import com.example.librewards.repositories.UserRepository
import com.example.librewards.utils.FragmentExtended
import com.example.librewards.utils.toastMessage
import com.example.librewards.viewmodels.MainSharedViewModel
import com.example.librewards.viewmodels.MainViewModelFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso

class RewardsFragment(override val icon: Int = R.drawable.reward) :
    FragmentExtended(),
    RecyclerAdapter.OnProductListener {
    private lateinit var userRepo: UserRepository

    private val mainSharedViewModel: MainSharedViewModel by activityViewModels {
        MainViewModelFactory(userRepo)
    }
    private lateinit var fh: FirebaseHandler
    private lateinit var mainActivity: MainActivity
    private lateinit var productEntries: MutableList<ProductEntry>
    private lateinit var productPopup: Dialog

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mainActivity = activity as MainActivity
        val database = FirebaseDatabase.getInstance().reference
        userRepo = UserRepository(database)
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        "See rewards from ${mainActivity.university}".also { binding.rewardsText.text = it }
        binding.rewardsRecycler.layoutManager = layoutManager
        productEntries = mutableListOf()
        val adapter = RecyclerAdapter(productEntries, this)
        binding.rewardsRecycler.adapter = adapter
        mainSharedViewModel.userPoints.observe(viewLifecycleOwner) { points ->
            binding.rewardsPoints.text = points
        }
        val productDb = FirebaseDatabase.getInstance().reference
            .child("products")
            .child(mainActivity.university)
        productDb.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productEntries.clear()
                for (dataSnapshot in snapshot.children) {
                    val productEntry = ProductEntry()
                    productEntry.id = dataSnapshot.key!!
                    productEntry.product = dataSnapshot.getValue(Product::class.java)!!
                    productEntries.add(productEntry)
                }
                adapter.notifyDataSetChanged()
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
            Integer.parseInt(binding.rewardsPoints.text.toString()) - Integer.parseInt(
                productEntries[position].product.productCost,
            )

        if (pointsInt > 0) {
            binding.rewardsPoints.text = pointsInt.toString()
            minusPointsListener(Integer.parseInt(productEntries[position].product.productCost))
        } else {
            toastMessage(requireActivity(), "You do not have sufficient points for this purchase")
        }
    }

    private fun showImagePopup(list: MutableList<ProductEntry>, position: Int) {
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
                0,
            )
        }

//         val drawableQR = mainActivity.qrCode.drawable
//         popupBinding.popupQr.setImageDrawable(drawableQR)

        popupBinding.popupQr.updateLayoutParams<ViewGroup.MarginLayoutParams> { setMargins(50) }
        popupBinding.popupText.text = list[position].product.productName
        "${list[position].product.productCost} points".also { popupBinding.popupCost.text = it }
        popupBinding.popupCost.textSize = 20F
        popupBinding.popupText.textSize = 25F
        popupBinding.popupCost.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            setMargins(
                0,
                0,
                0,
                50,
            )
        }
        Picasso.get().load(list[position].product.productImageUrl).into(popupBinding.popupImageView)

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

    private fun minusPointsListener(minusValue: Int) {
        val refChild = fh.getChild("users", mainActivity.email, "points")
        var dbPoints: String
        val pointsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dbPoints = dataSnapshot.value.toString()
                val finalPoints = Integer.parseInt(dbPoints) - minusValue
                refChild.setValue(finalPoints.toString())
                binding.rewardsPoints.text = finalPoints.toString()
                mainSharedViewModel.updatePoints(finalPoints)
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        }
        refChild.addListenerForSingleValueEvent(pointsListener)
    }

    companion object {
        val TAG: String = RewardsFragment::class.java.simpleName
    }

    override fun onProductClick(position: Int) {
        showImagePopup(productEntries, position)
    }
}
