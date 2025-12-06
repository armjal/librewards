package com.example.librewards

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.ImageDecoder
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.example.librewards.viewmodels.UiEvent
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.io.IOException

class AdminRewardsFragment(override val icon: Int = R.drawable.reward) : FragmentExtended(), RecyclerAdapter.OnProductListener {
    private val viewModel: AdminRewardsViewModel by viewModels {
        AdminRewardsViewModelFactory(productRepo, storageRepo)
    }
    private lateinit var database: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var adminActivity: AdminActivity
    private var popup: Dialog? = null

    private var imageLocalFilePath: Uri? = null
    private lateinit var productEntries: MutableList<ProductEntry>
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>? = null

    private var _binding: AdminFragmentRewardsBinding? = null
    private val binding get() = _binding!!

    private var addProductBinding: AddProductPopupBinding? = null
    private var manageProductBinding: ManageProductPopupBinding? = null
    private lateinit var productRepo: ProductRepository
    private lateinit var storageRepo: StorageRepository

    private val imagePickerLauncher = registerImagePickerLauncher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminActivity = activity as AdminActivity
        database = FirebaseDatabase.getInstance().reference.child("products")
            .child(adminActivity.university)
        storageReference = FirebaseStorage.getInstance().reference.child("products").child(
            "${adminActivity.university}/images/",
        )

        productRepo = ProductRepository(database)
        storageRepo = StorageRepository(storageReference)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = AdminFragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addAProduct.setOnClickListener { showAddProductPopup() }
        layoutManager = LinearLayoutManager(context)
        binding.adminRewardsRecycler.layoutManager = layoutManager
        productEntries = mutableListOf()
        adapter = RecyclerAdapter(productEntries, this)
        binding.adminRewardsRecycler.adapter = adapter

        viewModel.productEntries.observe(viewLifecycleOwner) {
            productEntries.clear()
            productEntries.addAll(it)
            (adapter as RecyclerAdapter).notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        addProductBinding = null
        manageProductBinding = null
    }

    private fun showAddProductPopup() {
        popup = Dialog(requireActivity())
        popup?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        addProductBinding = AddProductPopupBinding.inflate(layoutInflater)
        popup?.setContentView(addProductBinding!!.root)
        addProductBinding!!.let {
            it.chooseButton.setOnClickListener { fileChooser() }
            it.uploadButton.setOnClickListener { fileUploader() }
            it.closeBtnAdmin.setOnClickListener { popup?.dismiss() }
        }
        popup?.show()
    }

    private fun showManageProductPopup(list: List<ProductEntry>, position: Int) {
        val chosenProductEntry = list[position]
        popup = Dialog(requireActivity())
        popup?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        manageProductBinding = ManageProductPopupBinding.inflate(layoutInflater)
        popup?.setContentView(manageProductBinding!!.root)
        Picasso.get().load(list[position].product.productImageUrl)
            .into(manageProductBinding!!.manageProductImage)
        manageProductBinding!!.let {
            it.manageProductName.setText(list[position].product.productName)
            it.manageProductCost.setText(list[position].product.productCost)
            it.closeBtnManageAdmin.setOnClickListener { popup?.dismiss() }
            it.updateButton.setOnClickListener {
                chosenProductEntry.product.productName =
                    manageProductBinding!!.manageProductName.text.toString()
                chosenProductEntry.product.productCost =
                    manageProductBinding!!.manageProductCost.text.toString()

                updateProduct(chosenProductEntry)
            }
            it.deleteButton.setOnClickListener {
                deleteProduct(list[position])
            }
        }
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
        showProgressBar()
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
                        hideProgressBar()
                    }

                    is UiEvent.Failure -> {
                        Log.e(TAG, it.message)
                        hideProgressBar()
                        toastMessage(requireActivity(), "Product upload failed")
                    }
                }
            }
        }
    }

    private fun showProgressBar() {
        addProductBinding?.uploadProgressBar?.visibility = View.VISIBLE
        addProductBinding?.uploadButton?.isEnabled = false
    }

    private fun hideProgressBar() {
        addProductBinding?.uploadProgressBar?.visibility = View.GONE
        addProductBinding?.uploadButton?.isEnabled = true
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

    private fun registerImagePickerLauncher(): ActivityResultLauncher<Intent?> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                imageLocalFilePath = it.data?.data
                try {
                    val source = ImageDecoder.createSource(
                        requireActivity().contentResolver,
                        imageLocalFilePath!!,
                    )
                    val bitmap = ImageDecoder.decodeBitmap(source)
                    addProductBinding?.chosenImage?.let { img ->
                        img.layoutParams.height = 300
                        img.layoutParams.width = 300
                        img.setImageBitmap(bitmap)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

    companion object {
        private val TAG: String = AdminRewardsFragment::class.java.simpleName
    }

    override fun onProductClick(position: Int) {
        showManageProductPopup(productEntries, position)
    }
}
