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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.librewards.databinding.AddProductPopupBinding
import com.example.librewards.databinding.AdminFragmentRewardsBinding
import com.example.librewards.databinding.ManageProductPopupBinding
import com.example.librewards.models.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.IOException

class AdminRewardsFragment : Fragment(), RecyclerAdapter.OnProductListener {
    private lateinit var fh: FirebaseHandler
    private lateinit var database: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var adminActivity: AdminActivity
    private var popup: Dialog? = null

    private var filePath: Uri? = null
    private lateinit var productsList: MutableList<Product>
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>? = null

    private var _binding: AdminFragmentRewardsBinding? = null
    private val binding get() = _binding!!

    private var addProductBinding: AddProductPopupBinding? = null
    private var manageProductBinding: ManageProductPopupBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adminActivity = activity as AdminActivity
        fh = FirebaseHandler()
        database = FirebaseDatabase.getInstance().reference
        storageReference = FirebaseStorage.getInstance().reference
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
        productsList = mutableListOf()
        adapter = RecyclerAdapter(productsList, this)
        binding.adminRewardsRecycler.adapter = adapter
        database = FirebaseDatabase.getInstance().reference.child("products")
            .child(adminActivity.university)
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productsList.clear()
                for (dataSnapshot in snapshot.children) {
                    val product = dataSnapshot.getValue(Product::class.java)
                    productsList.add(product!!)

                }
                (adapter as RecyclerAdapter).notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Could not access database")
            }
        })
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
            filePath = data.data!!
            try {
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, filePath)
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

    private fun showManageProductPopup(list: List<Product>, position: Int) {
        val dbCurrentProduct = database
            .child(hashFunction(list[position].productName))
        popup = Dialog(requireActivity())
        popup?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        manageProductBinding = ManageProductPopupBinding.inflate(layoutInflater)
        popup?.setContentView(manageProductBinding!!.root)
        Picasso.get().load(list[position].productImageUrl)
            .into(manageProductBinding!!.manageProductImage)
        manageProductBinding!!.manageProductName.setText(list[position].productName)
        manageProductBinding!!.manageProductCost.setText(list[position].productCost)
        manageProductBinding!!.closeBtnManageAdmin.setOnClickListener { popup?.dismiss() }
        manageProductBinding!!.updateButton.setOnClickListener {
            dbCurrentProduct.child("productImageUrl")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val tempImageUrl = snapshot.value.toString()
                        val updatedProduct = Product(
                            manageProductBinding!!.manageProductName.text.toString(),
                            manageProductBinding!!.manageProductCost.text.toString(),
                            tempImageUrl
                        )
                        dbCurrentProduct.removeValue()
                        val updatedProductDb = database
                            .child(hashFunction(manageProductBinding!!.manageProductName.text.toString()))
                        updatedProductDb.setValue(updatedProduct)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Could not access database $error")
                    }
                })

        }
        manageProductBinding!!.deleteButton.setOnClickListener {
            dbCurrentProduct.removeValue()
            popup?.dismiss()
            Toast.makeText(requireActivity(), "Product successfully deleted", Toast.LENGTH_SHORT)
                .show()
        }

        popup?.show()
    }

    private fun fileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun fileUploader() {
        if (filePath == null) {
            Toast.makeText(context, "Please choose an image", Toast.LENGTH_SHORT).show()
            return
        }
        showProgressBar()
        val product = Product(
            addProductBinding!!.productName.text.toString(),
            addProductBinding!!.productCost.text.toString()
        )

        val imageRef = storageReference.child(
            "${adminActivity.university}/images/${hashFunction(filePath.toString())}-${
                product.productName.replace(' ', '-')
            }"
        )
        val uploadImageTask = imageRef.putFile(filePath!!)
        uploadImageTask.addOnSuccessListener {
            hideProgressBar()
            Toast.makeText(context, "File uploaded successfully", Toast.LENGTH_SHORT).show()
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                product.productImageUrl = uri.toString()
                setProductInfoInDb(product)
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

    private fun setProductInfoInDb(product: Product) {
        val refProduct = database.child(hashFunction(product.productName))

        refProduct.setValue(product)
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
        filePath = null
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1234
        private val TAG: String = AdminRewardsFragment::class.java.simpleName

    }

    override fun onProductClick(position: Int) {
        showManageProductPopup(productsList, position)
    }


}
