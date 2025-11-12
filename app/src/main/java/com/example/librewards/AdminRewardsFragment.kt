package com.example.librewards

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import com.example.librewards.models.Product
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.add_product_popup.*
import kotlinx.android.synthetic.main.admin_fragment_rewards.view.*
import kotlinx.android.synthetic.main.fragment_rewards.view.*
import kotlinx.android.synthetic.main.manage_product_popup.*
import java.io.IOException


class AdminRewardsFragment : Fragment(), RecyclerAdapter.OnProductListener {
    private val PICK_IMAGE_REQUEST = 1234
    private lateinit var fh: FirebaseHandler
    private lateinit var database: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var adminActivity: AdminActivity
    private lateinit var v: View
    private var popup: Dialog? = null
    private var popup1: Dialog? = null

    private var filePath: Uri? = null
    private lateinit var productsList: MutableList<Product>
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var adapter: RecyclerView.Adapter<RecyclerAdapter.ViewHolder>? = null

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
        v = inflater.inflate(R.layout.admin_fragment_rewards, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        v.addAProduct.setOnClickListener { showAddProductPopup() }
        layoutManager = LinearLayoutManager(context)
        view.adminRewardsRecycler.layoutManager = layoutManager
        productsList = mutableListOf()
        adapter = RecyclerAdapter(requireActivity(), productsList, this)
        view.adminRewardsRecycler.adapter = adapter
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
                popup!!.chosenImage.layoutParams.height = 300
                popup!!.chosenImage.layoutParams.width = 300
                popup!!.chosenImage.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    private fun showAddProductPopup() {
        popup = Dialog(requireActivity())
        popup?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup?.setContentView(R.layout.add_product_popup)
        popup?.chooseButton?.setOnClickListener { fileChooser() }
        popup?.uploadButton?.setOnClickListener { fileUploader() }
        popup?.closeBtnAdmin?.setOnClickListener { popup?.dismiss() }
        popup?.show()
    }

    private fun showManageProductPopup(list: MutableList<Product>, position: Int) {
        val dbCurrentProduct = database
            .child(fh.hashFunction(list[position].productname!!))
        popup = Dialog(requireActivity())
        popup?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup?.setContentView(R.layout.manage_product_popup)
        Picasso.get().load(list[position].productimage).into(popup!!.manageProductImage)
        popup!!.manageProductName.setText(list[position].productname)
        popup!!.manageProductCost.setText(list[position].productcost)
        popup!!.closeBtnManageAdmin.setOnClickListener { popup?.dismiss() }
        popup!!.updateButton.setOnClickListener {
            dbCurrentProduct.child("productimage")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val tempImageUrl = snapshot.value.toString()
                        val updatedProduct = Product(
                            popup!!.manageProductName.text.toString(),
                            popup!!.manageProductCost.text.toString(),
                            tempImageUrl
                        )
                        dbCurrentProduct.removeValue()
                        val updatedProductDb = database
                            .child(fh.hashFunction(popup!!.manageProductName.text.toString()))
                        updatedProductDb.setValue(updatedProduct)
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }

                })

        }
        popup!!.deleteButton.setOnClickListener {
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
            val refProduct = database.child(fh.hashFunction(popup!!.productName.text.toString()))
            val imageRef = storageReference.child(
                "${adminActivity.university}/images/${fh.hashFunction(filePath.toString())}-${
                    popup!!.productName.text.toString().replace(' ', '-')
                }"
            )
            imageRef.putFile(filePath!!)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Toast.makeText(context, "File Uploaded", Toast.LENGTH_SHORT).show()
                    imageRef.downloadUrl.addOnSuccessListener { taskSnapshot ->
                        val productImageUrl = taskSnapshot.toString()
                        refProduct.child("productname")
                            .setValue(popup!!.productName.text.toString())
                        refProduct.child("productcost")
                            .setValue(popup!!.productCost.text.toString())
                        refProduct.child("productimage").setValue(productImageUrl)
                        popup!!.productName.text.clear()
                        popup!!.productCost.text.clear()
                        popup!!.chosenImage.layoutParams.height = 0
                        popup!!.chosenImage.layoutParams.width = 0
                        popup!!.chosenImage.setImageDrawable(null)

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
        val TAG: String = AdminRewardsFragment::class.java.simpleName

    }

    override fun onProductClick(position: Int) {
        showManageProductPopup(productsList, position)
    }


}