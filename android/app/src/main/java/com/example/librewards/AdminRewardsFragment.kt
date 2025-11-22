package com.example.librewards

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.librewards.databinding.AddProductPopupBinding
import com.example.librewards.databinding.AdminFragmentRewardsBinding
import com.example.librewards.databinding.ManageProductPopupBinding
import com.example.librewards.models.Product
import com.example.librewards.models.ProductEntry
import com.example.librewards.repositories.ProductRepository
import com.example.librewards.viewmodels.AdminRewardsViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.IOException
import java.util.UUID

class AdminRewardsFragment : Fragment(), RecyclerAdapter.OnProductListener {
    private val viewModel: AdminRewardsViewModel by viewModels {
        AdminRewardsViewModel.AdminRewardsViewModelFactory(productRepo)
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminActivity = activity as AdminActivity
        database = FirebaseDatabase.getInstance().reference.child("products")
            .child(adminActivity.university)
        storageReference = FirebaseStorage.getInstance().reference.child("products").child(
            "${adminActivity.university}/images/"
        )

        productRepo = ProductRepository(database)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST &&
            resultCode == AppCompatActivity.RESULT_OK &&
            data != null &&
            data.data != null
        ) {
            imageLocalFilePath = data.data!!
            try {
                val bitmap =
                    MediaStore.Images.Media.getBitmap(
                        requireActivity().contentResolver,
                        imageLocalFilePath
                    )
                addProductBinding?.chosenImage?.let {
                    it.layoutParams.height = 300
                    it.layoutParams.width = 300
                    it.setImageBitmap(bitmap)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    private fun showAddProductPopup() {
        popup = Dialog(requireActivity())
        popup?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        addProductBinding = AddProductPopupBinding.inflate(layoutInflater)
        popup?.setContentView(addProductBinding!!.root)
        addProductBinding!!.chooseButton.setOnClickListener { fileChooser() }
        addProductBinding!!.uploadButton.setOnClickListener { fileUploader() }
        addProductBinding!!.closeBtnAdmin.setOnClickListener { popup?.dismiss() }
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
            it.updateButton.setOnClickListener { its ->
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
        productRepo.updateProduct(chosenProductEntry).addOnSuccessListener {
            popup?.dismiss()
            Toast.makeText(
                requireActivity(),
                "Product successfully updated",
                Toast.LENGTH_SHORT
            ).show()
        }.addOnFailureListener { error ->
            Log.e(TAG, "Failed to update product: $error")
            Toast.makeText(requireActivity(), "Update failed", Toast.LENGTH_SHORT).show()

        }
    }

    private fun deleteProduct(chosenProductEntry: ProductEntry) {
        productRepo.deleteProduct(chosenProductEntry)
        popup?.dismiss()
        Toast.makeText(requireActivity(), "Product successfully deleted", Toast.LENGTH_SHORT)
            .show()
    }

    private fun fileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun fileUploader() {
        if (imageLocalFilePath == null) {
            Toast.makeText(context, "Please choose an image", Toast.LENGTH_SHORT).show()
            return
        }
        showProgressBar()
        val productEntry = ProductEntry(
            generateProductId(),
            Product(
                addProductBinding!!.productName.text.toString(),
                addProductBinding!!.productCost.text.toString()
            )
        )

        val imageRef = storageReference.child(
            "${hashFunction(imageLocalFilePath.toString())}-${
                productEntry.id
            }"
        )
        val uploadImageTask = imageRef.putFile(imageLocalFilePath!!)
        uploadImageTask.addOnSuccessListener {
            hideProgressBar()
            Toast.makeText(context, "File uploaded successfully", Toast.LENGTH_SHORT).show()
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                productEntry.product.productImageUrl = uri.toString()
                productRepo.addProductToDb(productEntry)
                resetProductInputFields()
            }
        }
            .addOnFailureListener {
                hideProgressBar()
                Toast.makeText(context, "Failed to upload product image", Toast.LENGTH_SHORT).show()
            }
            .addOnProgressListener { _ ->
            }
    }

    private fun generateProductId(): String {
        return "PROD-" + UUID.randomUUID().toString()
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

    companion object {
        private const val PICK_IMAGE_REQUEST = 1234
        private val TAG: String = AdminRewardsFragment::class.java.simpleName

    }

    override fun onProductClick(position: Int) {
        showManageProductPopup(productEntries, position)
    }


}
