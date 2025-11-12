package com.example.librewards

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.librewards.models.User
import com.facebook.*
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*


class Login : AppCompatActivity() {
    private lateinit var facebookFirstName: String
    private lateinit var facebookLastName: String
    private lateinit var facebookEmail: String
    private lateinit var facebookPhotoURL: String
    private lateinit var id: String
    private lateinit var callbackManager: CallbackManager
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var fh: FirebaseHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        fh = FirebaseHandler()
        auth = Firebase.auth

        FacebookSdk.sdkInitialize(this.applicationContext)
        AppEventsLogger.activateApp(application)
        callbackManager = CallbackManager.Factory.create()
        database = FirebaseDatabase.getInstance().reference

        facebookLoginButton.setPermissions(listOf("public_profile", "email"))

        registerButton.setOnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            if (loginEmail.text.toString() == "" || loginPassword.text.toString() == "") {
                Toast.makeText(baseContext, "Please ensure all fields are correctly filled out.", Toast.LENGTH_SHORT).show()
            } else {
                signIn()
            }
        }

        facebookLoginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
                val facebookUniversity = FacebookUniversity()
                getFacebookInfo(loginResult.accessToken, facebookUniversity)

            }

            override fun onCancel() {
                Log.d(TAG, "Facebook onCancel.")
                Toast.makeText(this@Login, "Login Cancelled", Toast.LENGTH_LONG).show()
            }

            override fun onError(exception: FacebookException) {
                Log.d(TAG, "Facebook onError.")
                Toast.makeText(this@Login, exception.message, Toast.LENGTH_LONG).show()
            }
        })

    }

    private fun getUserLoginInfo(email: String, activity: AppCompatActivity) {
        val id = fh.hashFunction(email)
        val refChild = database.child("users").child(id)
        refChild.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                val intent = Intent(this@Login, activity::class.java)
                intent.putExtra("email", user?.email)
                intent.putExtra("first_name", user?.firstname)
                intent.putExtra("last_name", user?.surname)
                intent.putExtra("university", user?.university)
                startActivity(intent)
                finish()

            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun getFacebookInfo(token: AccessToken, activity: AppCompatActivity) {
        val request = GraphRequest.newMeRequest(token) { `object`, response ->
            Log.d(TAG, `object`.toString())
            if (`object`.has("first_name")) {
                facebookFirstName = `object`.getString("first_name")
            }
            if (`object`.has("last_name")) {
                facebookLastName = `object`.getString("last_name")
            }
            if (`object`.has("email")) {
                facebookEmail = `object`.getString("email")
            }
            if (`object`.has("id")) {
                id = `object`.getString("id")
                facebookPhotoURL = "https://graph.facebook.com/$id/picture?type=normal"
            }
            var university: String
            val refChild = fh.getChild("users", facebookEmail, "university")
            refChild.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    university = dataSnapshot.value.toString()
                    var intent = Intent(this@Login, activity::class.java)
                    if(university != "null" && university != ""){
                        intent = Intent(this@Login,MainActivity::class.java)
                        intent.putExtra("university", university)
                    }
                    intent.putExtra("email", facebookEmail)
                    intent.putExtra("first_name", facebookFirstName)
                    intent.putExtra("last_name", facebookLastName)
                    intent.putExtra("photo", facebookPhotoURL)
                    startActivity(intent)
                    finish()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Log.w(TAG, "Failed to read value.", error.toException())
                }
            })

        }
        val parameters = Bundle()
        parameters.putString("fields", "id,first_name,last_name,email")
        request.parameters = parameters
        request.executeAsync()
    }

    public override fun onStart() {
        super.onStart()

        if(!isFacebookUserLoggedIn()) {
            val currentUser = auth.currentUser
            updateUI(currentUser)
            // Check if user is signed in (non-null) and update UI accordingly.
            if (currentUser != null) {
                var isAdmin: String
                val refChild = fh.getChild("users", currentUser.email!!, "admin")
                refChild.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        isAdmin = dataSnapshot.value.toString()
                        if (isAdmin == "0") {
                            val intent = Intent(this@Login, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else if (isAdmin == "1") {
                            getUserLoginInfo(currentUser.email!!, AdminActivity())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Failed to read value
                        Log.w(TAG, "Failed to read value.", error.toException())
                    }
                })
                reload()
            }
        }
        else{
            val mainActivity = MainActivity()
            getFacebookInfo(AccessToken.getCurrentAccessToken(), mainActivity)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun signIn() {
        auth.signInWithEmailAndPassword(loginEmail.text.toString(), loginPassword.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        var isAdmin: String
                        val refChild = fh.getChild("users", loginEmail.text.toString(), "admin")
                        refChild.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                isAdmin = dataSnapshot.value.toString()
                                if (isAdmin == "0") {
                                    val intent = Intent(this@Login, MainActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else if (isAdmin == "1") {
                                    getUserLoginInfo(loginEmail.text.toString(), AdminActivity())
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Failed to read value
                                Log.w(TAG, "Failed to read value.", error.toException())
                            }
                        })
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
    }

    private fun isFacebookUserLoggedIn(): Boolean {
        return AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired
    }


    private fun updateUI(user: FirebaseUser?) {

    }

    private fun reload() {

    }

    fun login(view: View) {}

    companion object {
        val TAG = Login::class.java.simpleName
    }

}