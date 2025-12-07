package com.example.librewards

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
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

import com.example.librewards.viewmodels.RewardsViewModel
import com.example.librewards.viewmodels.RewardsViewModelFactory

import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class RewardsFragment(override val icon: Int = R.drawable.reward) :
    FragmentExtended(),
    RecyclerAdapter.OnProductListener {
    private lateinit var productRepo: ProductRepository
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val rewardsViewModel: RewardsViewModel by viewModels {
        RewardsViewModelFactory(mainActivity.userRepo, productRepo)
    }
    private lateinit var productDatabase: DatabaseReference
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
        productDatabase = FirebaseDatabase.getInstance().reference
            .child("products")
            .child(mainActivity.university)
        productRepo = ProductRepository(productDatabase)
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(context)
        "See rewards from ${mainActivity.university}".also { binding.rewardsText.text = it }
        binding.rewardsRecycler.layoutManager = layoutManager
        mainSharedViewModel.userPoints.observe(viewLifecycleOwner) { points ->
            binding.rewardsPoints.text = points
        }

        productEntries = mutableListOf()
        val adapter = RecyclerAdapter(productEntries, this)
        binding.rewardsRecycler.adapter = adapter

        rewardsViewModel.productEntries.observe(viewLifecycleOwner) {
            productEntries.clear()
            productEntries.addAll(it)
            adapter.notifyDataSetChanged()
        }

        rewardsViewModel.listenForRewardRedemption(mainActivity.email)
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
            mainSharedViewModel.minusPoints(
                Integer.parseInt(productEntries[position].product.productCost),
            )
        } else {
            toastMessage(requireActivity(), "You do not have sufficient points for this purchase")
        }
    }

    private fun showImagePopup(list: MutableList<ProductEntry>, position: Int) {
        productPopup = Dialog(requireActivity())
        productPopup.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
//        rewardsViewModel.updateRewardRedeemed(mainSharedViewModel.updatedUser.value!!, "0")
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

        rewardsViewModel.rewardStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                RewardsEvent.Redeemed -> {
                    calculatePointsFromPurchase(position)
//                    rewardsViewModel.updateRewardRedeemed(mainSharedViewModel.updatedUser.value!!, "0")
                }

                else -> {}
            }
        }

        popupBinding.closeBtn.setOnClickListener {
            productPopup.dismiss()
        }
//        productPopup.setOnDismissListener { rewardsViewModel.updateRewardRedeemed(mainSharedViewModel.updatedUser.value!!, "2") }
        productPopup.show()
    }

    companion object {
        val TAG: String = RewardsFragment::class.java.simpleName
    }

    override fun onProductClick(position: Int) {
        showImagePopup(productEntries, position)
    }
}
