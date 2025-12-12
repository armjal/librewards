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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.librewards.databinding.FragmentRewardsBinding
import com.example.librewards.databinding.PopupLayoutBinding
import com.example.librewards.models.ProductEntry
import com.example.librewards.repositories.ProductRepository
import com.example.librewards.utils.FragmentExtended
import com.example.librewards.utils.toastMessage
import com.example.librewards.viewmodels.MainSharedViewModel
import com.example.librewards.viewmodels.RewardsEvent
import com.example.librewards.viewmodels.RewardsViewModel
import com.example.librewards.viewmodels.RewardsViewModelFactory
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso

class RewardsFragment(override val icon: Int = R.drawable.reward) :
    FragmentExtended(),
    RecyclerAdapter.OnProductListener {
    companion object {
        val TAG: String = RewardsFragment::class.java.simpleName
    }

    private lateinit var productRepo: ProductRepository
    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val rewardsViewModel: RewardsViewModel by viewModels {
        RewardsViewModelFactory(mainSharedViewModel, productRepo)
    }
    private lateinit var mainActivity: MainActivity
    private lateinit var productEntries: MutableList<ProductEntry>
    private lateinit var productPopup: Dialog

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!
    private lateinit var popupBinding: PopupLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mainActivity = activity as MainActivity
        val productDatabase = FirebaseDatabase.getInstance().reference
            .child("products")
            .child(mainActivity.university)
        productRepo = ProductRepository(productDatabase)
        setupProductPopupUI()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showProductPopup(list: MutableList<ProductEntry>, position: Int) {
        with(popupBinding) {
            popupText.text = list[position].product.productName
            "${list[position].product.productCost} points".also { popupCost.text = it }

            Picasso.get().load(list[position].product.productImageUrl).into(popupImageView)
        }
        productPopup.show()
    }

    private fun setupProductPopupUI() {
        productPopup = Dialog(requireActivity())
        productPopup.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupBinding = PopupLayoutBinding.inflate(layoutInflater)
        productPopup.setContentView(popupBinding.root)

//         val drawableQR = mainActivity.qrCode.drawable
//         popupBinding.popupQr.setImageDrawable(drawableQR)

        with(popupBinding) {
            popupImageView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                height = 150
                width = 150
                setMargins(30, 0, 0, 0)
            }
            popupQr.updateLayoutParams<ViewGroup.MarginLayoutParams> { setMargins(50) }
            popupText.textSize = 25F
            popupCost.textSize = 20F
            popupCost.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                setMargins(0, 0, 0, 50)
            }
            closeBtn.setOnClickListener {
                productPopup.dismiss()
            }
        }
    }

    fun observeRewardsStatus(productPosition: Int) {
        rewardsViewModel.rewardStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                RewardsEvent.Redeemed -> {
                    rewardsViewModel.minusPoints(
                        Integer.parseInt(productEntries[productPosition].product.productCost),
                    )
                    rewardsViewModel.setRewardsStatus(RewardsEvent.ReadyToRedeem)
                }

                RewardsEvent.InsufficientFunds -> {
                    toastMessage(requireActivity(), getString(R.string.insufficient_funds))
                }

                else -> {}
            }
        }
    }

    override fun onProductClick(position: Int) {
        rewardsViewModel.setRewardsStatus(RewardsEvent.ReadyToRedeem)
        observeRewardsStatus(position)
        showProductPopup(productEntries, position)
        productPopup.setOnDismissListener { rewardsViewModel.setRewardsStatus(RewardsEvent.Neutral) }
    }
}
