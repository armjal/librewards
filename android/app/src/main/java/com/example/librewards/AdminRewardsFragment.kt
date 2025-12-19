package com.example.librewards

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.example.librewards.databinding.AddProductPopupBinding
import com.example.librewards.databinding.AdminFragmentRewardsBinding
import com.example.librewards.databinding.ManageProductPopupBinding
import com.example.librewards.models.ImageFile
import com.example.librewards.models.Product
import com.example.librewards.models.ProductEntry
import com.example.librewards.repositories.ProductRepository
import com.example.librewards.repositories.StorageRepository
import com.example.librewards.utils.FragmentExtended
import com.example.librewards.utils.toastMessage
import com.example.librewards.viewmodels.AdminRewardsViewModel
import com.example.librewards.viewmodels.AdminRewardsViewModelFactory
import com.example.librewards.viewmodels.AdminViewModel
import com.example.librewards.viewmodels.UiEvent
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.io.IOException

class AdminRewardsFragment(override val icon: Int = R.drawable.reward) : FragmentExtended(), RecyclerAdapter.OnProductListener {
    companion object {
        private val TAG: String = AdminRewardsFragment::class.java.simpleName
    }

    private val viewModel: AdminRewardsViewModel by viewModels {
        val database = FirebaseDatabase.getInstance().reference.child("products")
            .child(adminViewModel.user.value?.university!!)
        val storageReference = FirebaseStorage.getInstance().reference.child("products").child(
            "${adminViewModel.user.value?.university!!}/images/",
        )
        AdminRewardsViewModelFactory(ProductRepository(database), StorageRepository(storageReference))
    }

    private val adminViewModel: AdminViewModel by activityViewModels()
    private var popup: Dialog? = null
    private var imageLocalFilePath: Uri? = null
    private var _binding: AdminFragmentRewardsBinding? = null
    private val binding get() = _binding!!
    private var addProductBinding: AddProductPopupBinding? = null
    private var manageProductBinding: ManageProductPopupBinding? = null
    private val imagePickerLauncher = getImagePickerLauncher()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AdminFragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addAProduct.setOnClickListener {
            showAddProductPopup()
        }
        val layoutManager = LinearLayoutManager(context)
        binding.adminRewardsRecycler.layoutManager = layoutManager
        val adapter = RecyclerAdapter(mutableListOf(), this)
        binding.adminRewardsRecycler.adapter = adapter
        viewModel.productEntries.observe(viewLifecycleOwner) {
            adapter.updateList(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        addProductBinding = null
        manageProductBinding = null
    }

    private fun showAddProductPopup() {
        addProductBinding = AddProductPopupBinding.inflate(layoutInflater)
        showPopupWithContents(addProductBinding!!) {
            with(addProductBinding!!) {
                chooseButton.setOnClickListener { fileChooser() }
                uploadButton.setOnClickListener { fileUploader() }
                closeBtnAdmin.setOnClickListener { popup?.dismiss() }
            }
        }
    }

    private fun showManageProductPopup(productEntry: ProductEntry) {
        val chosenProduct = productEntry.product
        manageProductBinding = ManageProductPopupBinding.inflate(layoutInflater)
        showPopupWithContents(manageProductBinding!!) {
            with(manageProductBinding!!) {
                Picasso.get().load(chosenProduct.productImageUrl).into(manageProductImage)
                manageProductName.setText(chosenProduct.productName)
                manageProductCost.setText(chosenProduct.productCost)
                closeBtnManageAdmin.setOnClickListener { popup?.dismiss() }
                updateButton.setOnClickListener {
                    chosenProduct.productName = manageProductName.text.toString()
                    chosenProduct.productCost = manageProductCost.text.toString()
                    updateProduct(productEntry)
                }
                deleteButton.setOnClickListener {
                    deleteProduct(productEntry)
                }
            }
        }
    }

    private fun showPopupWithContents(binding: ViewBinding, addContent: () -> Unit) {
        popup = Dialog(requireActivity())
        popup?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        popup?.setContentView(binding.root)
        addContent()
        popup?.show()
    }

    private fun updateProduct(chosenProductEntry: ProductEntry) {
        lifecycleScope.launch {
            viewModel.updateProductEntry(chosenProductEntry).collect {
                when (it) {
                    is UiEvent.Success -> {
                        popup?.dismiss()
                        toastMessage(requireActivity(), it.message)
                    }

                    is UiEvent.Failure -> {
                        Log.e(TAG, it.message)
                        toastMessage(requireActivity(), "Product update failed")
                    }
                }
            }
        }
    }

    private fun deleteProduct(chosenProductEntry: ProductEntry) {
        lifecycleScope.launch {
            viewModel.deleteProductEntry(chosenProductEntry.id).collect {
                when (it) {
                    is UiEvent.Success -> {
                        popup?.dismiss()
                        toastMessage(requireActivity(), it.message)
                    }

                    is UiEvent.Failure -> {
                        Log.e(TAG, it.message)
                        toastMessage(requireActivity(), "Product deletion failed")
                    }
                }
            }
        }
    }

    private fun fileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        imagePickerLauncher.launch(intent)
    }

    private fun fileUploader() {
        if (imageLocalFilePath == null) {
            toastMessage(requireActivity(), "Please choose an image")
            return
        }
        setProgressState(isLoading = true)
        val product = Product(
            addProductBinding!!.productName.text.toString(),
            addProductBinding!!.productCost.text.toString(),
        )
        val imageFile =
            ImageFile(name = hashFunction(product.productName), uri = imageLocalFilePath)

        addProduct(product, imageFile)
    }

    private fun addProduct(product: Product, imageFile: ImageFile) {
        lifecycleScope.launch {
            viewModel.addProductEntry(product, imageFile).collect {
                when (it) {
                    is UiEvent.Success -> {
                        toastMessage(requireActivity(), it.message)
                        resetProductInputFields()
                        setProgressState(isLoading = false)
                    }

                    is UiEvent.Failure -> {
                        Log.e(TAG, it.message)
                        setProgressState(isLoading = false)
                        toastMessage(requireActivity(), "Product upload failed")
                    }
                }
            }
        }
    }

    private fun setProgressState(isLoading: Boolean) {
        addProductBinding?.let { binding ->
            binding.uploadProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.uploadButton.isEnabled = !isLoading
        }
    }

    private fun resetProductInputFields() {
        addProductBinding?.let {
            it.productName.text.clear()
            it.productCost.text.clear()
            it.chosenImage.layoutParams.height = 0
            it.chosenImage.layoutParams.width = 0
            it.chosenImage.setImageDrawable(null)
        }
        imageLocalFilePath = null
    }

    private fun getImagePickerLauncher(): ActivityResultLauncher<Intent?> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != AppCompatActivity.RESULT_OK) return@registerForActivityResult
            imageLocalFilePath = it.data?.data
            try {
                Picasso.get()
                    .load(imageLocalFilePath)
                    .resize(300, 300)
                    .centerCrop()
                    .into(addProductBinding?.chosenImage)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    override fun onProductClick(position: Int) {
        val chosenProductEntry = viewModel.productEntries.value?.get(position)!!
        showManageProductPopup(chosenProductEntry)
    }
}
