package com.example.librewards

import android.app.Dialog
import android.app.ProgressDialog
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
import androidx.core.graphics.drawable.toDrawable


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

    private fun showManageProductPopup(list: MutableList<Product>, position: Int) {
        val dbCurrentProduct = database
            .child(fh.hashFunction(list[position].productName!!))
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
            dbCurrentProduct.child("productImage")
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
                            .child(fh.hashFunction(manageProductBinding!!.manageProductName.text.toString()))
                        updatedProductDb.setValue(updatedProduct)
                    }

                    override fun onCancelled(error: DatabaseError) {
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
        if (filePath != null) {
            val progressDialog = ProgressDialog(requireActivity())
            progressDialog.setTitle("Uploading...")
            progressDialog.show()
            val refProduct =
                database.child(fh.hashFunction(addProductBinding!!.productName.text.toString()))
            val imageRef = storageReference.child(
                "${adminActivity.university}/images/${fh.hashFunction(filePath.toString())}-${
                    addProductBinding!!.productName.text.toString().replace(' ', '-')
                }"
            )
            imageRef.putFile(filePath!!)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(context, "File Uploaded", Toast.LENGTH_SHORT).show()
                    imageRef.downloadUrl.addOnSuccessListener { taskSnapshot ->
                        val productImageUrl = taskSnapshot.toString()
                        refProduct.child("productName")
                            .setValue(addProductBinding!!.productName.text.toString())
                        refProduct.child("productCost")
                            .setValue(addProductBinding!!.productCost.text.toString())
                        refProduct.child("productImage").setValue(productImageUrl)
                        addProductBinding?.let {
                            it.productName.text.clear()
                            it.productCost.text.clear()
                            it.chosenImage.layoutParams.height = 0
                            it.chosenImage.layoutParams.width = 0
                            it.chosenImage.setImageDrawable(null)
                        }

                    }
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapShot ->
                    val progress =
                        100.0 * taskSnapShot.bytesTransferred / taskSnapShot.totalByteCount
                    progressDialog.setMessage("Uploaded " + progress.toInt() + "%...")
                }

        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1234
        private val TAG: String = AdminRewardsFragment::class.java.simpleName

    }

    override fun onProductClick(position: Int) {
        showManageProductPopup(productsList, position)
    }


}
