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
import com.example.librewards.models.Product
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

    private val mainSharedViewModel: MainSharedViewModel by activityViewModels()
    private val rewardsViewModel: RewardsViewModel by viewModels {
        val productDatabase = FirebaseDatabase.getInstance().reference
            .child("products")
            .child(mainSharedViewModel.user.value?.university!!)
        val productRepo = ProductRepository(productDatabase)
        RewardsViewModelFactory(mainSharedViewModel, productRepo)
    }

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!
    private lateinit var productPopup: Dialog
    private lateinit var popupBinding: PopupLayoutBinding
    private lateinit var recyclerAdapter: RecyclerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rewardsText.text = getString(R.string.rewards_from_university, mainSharedViewModel.user.value!!.university)
        setupProductPopupUI()
        setupRecyclerAdapter()
        setupObservers()
    }

    fun setupRecyclerAdapter() {
        recyclerAdapter = RecyclerAdapter(mutableListOf(), this)
        with(binding.rewardsRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = recyclerAdapter
        }
    }

    fun setupObservers() {
        mainSharedViewModel.userPoints.observe(viewLifecycleOwner) { points ->
            binding.rewardsPoints.text = points
        }

        rewardsViewModel.productEntries.observe(viewLifecycleOwner) {
            recyclerAdapter.updateList(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showProductPopup(chosenProduct: Product) {
        with(popupBinding) {
            popupText.text = chosenProduct.productName
            popupCost.text = "${chosenProduct.productCost} points"

            Picasso.get().load(chosenProduct.productImageUrl).into(popupImageView)
        }
        productPopup.show()
    }

    private fun setupProductPopupUI() {
        productPopup = Dialog(requireActivity())
        productPopup.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popupBinding = PopupLayoutBinding.inflate(layoutInflater)
        productPopup.setContentView(popupBinding.root)

        observerUserQRCode()

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

    private fun observerUserQRCode() {
        mainSharedViewModel.userQrCode.observe(viewLifecycleOwner) { qrCode ->
            popupBinding.popupQr.setImageBitmap(qrCode.bitmap)
        }
    }

    fun observeRewardsStatus(chosenProduct: Product) {
        rewardsViewModel.rewardStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                RewardsEvent.Redeemed -> {
                    rewardsViewModel.minusPoints(
                        Integer.parseInt(chosenProduct.productCost),
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
        val chosenProduct = rewardsViewModel.productEntries.value!![position].product
        rewardsViewModel.setRewardsStatus(RewardsEvent.ReadyToRedeem)
        observeRewardsStatus(chosenProduct)
        showProductPopup(chosenProduct)
        productPopup.setOnDismissListener { rewardsViewModel.setRewardsStatus(RewardsEvent.Neutral) }
    }
}
