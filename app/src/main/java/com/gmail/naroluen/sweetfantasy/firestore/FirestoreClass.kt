package com.gmail.naroluen.sweetfantasy.firestore

import android.app.Activity
import androidx.fragment.app.Fragment
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.gmail.naroluen.sweetfantasy.model.Product
import com.gmail.naroluen.sweetfantasy.model.User
import com.gmail.naroluen.sweetfantasy.ui.activities.*
import com.gmail.naroluen.sweetfantasy.ui.fragments.DashboardFragment
import com.gmail.naroluen.sweetfantasy.ui.fragments.ProductsFragment
import com.gmail.naroluen.sweetfantasy.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class FirestoreClass {

    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: RegisterActivity, userInfo: User) {
        mFireStore.collection(Constants.USERS)
            // Document ID for users fields. Here the document it is the User ID.
            .document(userInfo.id)
            // UserInfo are Field and the SetOption is set to merge. It is for if we want to merge later on instead of replacing the fields.
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegistrationSuccess()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error en el proceso de registro.",
                    e
                )
            }
    }

    fun getCurrentUserID(): String {
        // An Instance of currentUser using FirebaseAuth
        val currentUser = FirebaseAuth.getInstance().currentUser

        // A variable to assign the currentUserId if it is not null or else it will be blank.
        var currentUserID = ""
        if (currentUser != null) {
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun getUserDetails(activity: Activity) {
        // Here we pass the collection name from which we wants the data.
        mFireStore.collection(Constants.USERS)
            // The document id to get the Fields of user.
            .document(getCurrentUserID())
            .get()
            .addOnSuccessListener { document ->

                Log.i(activity.javaClass.simpleName, document.toString())

                // Here we have received the document snapshot which is converted into the User Data model object.
                val user = document.toObject(User::class.java)!!

                val sharedPreferences =
                    activity.getSharedPreferences(
                        Constants.SWEETFANTASY_PREFERENCES,
                        Context.MODE_PRIVATE
                    )

                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putString(
                    Constants.LOGGED_IN_USERNAME,
                    "${user.firstName} ${user.lastName}"
                )
                editor.apply()

                when (activity) {
                    is LoginActivity -> {
                        // Call a function of base activity for transferring the result to it.
                        activity.userLoggedInSuccess(user)
                    }
                    is SettingsActivity ->{
                        activity.userDetailsSuccess(user)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Hide the progress dialog if there is any error. And print the error in log.
                when (activity) {
                    is LoginActivity -> {
                        activity.hideProgressDialog()
                    }
                    is SettingsActivity -> {
                        activity.hideProgressDialog()
                    }
                }

                Log.e(
                    activity.javaClass.simpleName,
                    "Error while getting user details.",
                    e
                )
            }
    }

    fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>) {
        // Collection Name
        mFireStore.collection(Constants.USERS)
            // Document ID against which the data to be updated. Here the document id is the current logged in user id.
            .document(getCurrentUserID())
            // A HashMap of fields which are to be updated.
            .update(userHashMap)
            .addOnSuccessListener {
                // Notify the success result.
                when (activity) {
                    is UserProfileActivity -> {
                        // Call a function of base activity for transferring the result to it.
                        activity.userProfileUpdateSuccess()
                    }
                }
            }
            .addOnFailureListener { e ->
                when (activity) {
                    is UserProfileActivity -> {
                        // Hide the progress dialog if there is any error. And print the error in log.
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "Error en actualización de Perfil", e)
            }
    }

    fun uploadImageToCloudStorage(activity: Activity, imageFileURI: Uri?, imageType: String) {
        //getting the storage reference
        val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                imageType + System.currentTimeMillis() + "."
                        + Constants.getFileExtension(
                        activity,
                        imageFileURI
            )
        )
        //adding the file to reference
        sRef.putFile(imageFileURI!!)
            .addOnSuccessListener { taskSnapshot ->
                // The image upload is successful
                Log.e(
                    "Firebase Image URL",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )
                // Get the downloadable url from the task snapshot
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        Log.e("Downloadable Image URL", uri.toString())
                        // Here call a function of base activity for transferring the result to it.
                        when (activity) {
                            is UserProfileActivity -> {
                                activity.imageUploadSuccess(uri.toString())
                            }
                            is AddProductActivity -> {
                                activity.imageUploadSuccess(uri.toString())
                            }
                        }
                    }
            }
            .addOnFailureListener { exception ->
                // Hide the progress dialog if there is any error. And print the error in log.
                when (activity) {
                    is UserProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                    is AddProductActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(
                    activity.javaClass.simpleName,
                    exception.message,
                    exception
                )
            }
    }

    /**
     * A function to make an entry of the user's product in the cloud firestore database.
     */
    fun uploadProductDetails(activity: AddProductActivity, productInfo: Product) {
        mFireStore.collection(Constants.PRODUCTS)
                .document()
                // Here the userInfo are Field and the SetOption is set to merge. It is for if we wants to merge
                .set(productInfo, SetOptions.merge())
                .addOnSuccessListener {
                    // Here call a function of base activity for transferring the result to it.
                    activity.productUploadSuccess()
                }
                .addOnFailureListener { e ->
                    activity.hideProgressDialog()
                    Log.e(
                            activity.javaClass.simpleName,
                            "Error al guardar los detalles de artículo",
                            e
                    )
                }
    }

    /**
     * A function to get the products list from cloud firestore.
     *
     * @param fragment The fragment is passed as parameter as the function is called from fragment and need to the success result.
     */
    fun getProductsList(fragment: Fragment) {
        // The collection name for PRODUCTS
        mFireStore.collection(Constants.PRODUCTS)
                .whereEqualTo(Constants.USER_ID, getCurrentUserID())
                .get() // Will get the documents snapshots.
                .addOnSuccessListener { document ->
                    // Here we get the list of boards in the form of documents.
                    Log.e("Products List", document.documents.toString())
                    // Here we have created a new instance for Products ArrayList.
                    val productsList: ArrayList<Product> = ArrayList()
                    // A for loop as per the list of documents to convert them into Products ArrayList.
                    for (i in document.documents) {
                        val product = i.toObject(Product::class.java)
                        product!!.product_id = i.id
                        productsList.add(product)
                    }
                    when (fragment) {
                        is ProductsFragment -> {
                            fragment.successProductsListFromFireStore(productsList)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Hide the progress dialog if there is any error based on the base class instance.
                    when (fragment) {
                        is ProductsFragment -> {
                            fragment.hideProgressDialog()
                        }
                    }
                    Log.e("Get Product List", "Error while getting product list.", e)
                }
    }

    /**
     * A function to delete the product from the cloud firestore.
     */
    fun deleteProduct(fragment: ProductsFragment, productId: String) {
        //ACCESS FIRESTORE DB
        mFireStore.collection(Constants.PRODUCTS)
                .document(productId)
                .delete()
                .addOnSuccessListener {
                    // Notify the success result to the base class.
                    fragment.productDeleteSuccess()
                }
                .addOnFailureListener { e ->
                    // Hide the progress dialog if there is an error.
                    fragment.hideProgressDialog()
                    Log.e(
                            fragment.requireActivity().javaClass.simpleName,
                            "Error al eliminar artículo",
                            e
                    )
                }
    }

    /**
     * A function to get the dashboard items list. The list will be an overall items list, not based on the user's id.
     */
    fun getDashboardItemsList(fragment: DashboardFragment) {
        // The collection name for PRODUCTS
        mFireStore.collection(Constants.PRODUCTS)
                .get() // Will get the documents snapshots.
                .addOnSuccessListener { document ->
                    // Here we get the list of boards in the form of documents.
                    Log.e(fragment.javaClass.simpleName, document.documents.toString())

                    // Here we have created a new instance for Products ArrayList.
                    val productsList: ArrayList<Product> = ArrayList()

                    // A for loop as per the list of documents to convert them into Products ArrayList.
                    for (i in document.documents) {

                        val product = i.toObject(Product::class.java)!!
                        product.product_id = i.id
                        productsList.add(product)
                    }
                    // Pass the success result to the base fragment.
                    fragment.successDashboardItemsList(productsList)
                }
                .addOnFailureListener { e ->
                    // Hide the progress dialog if there is any error which getting the dashboard items list.
                    fragment.hideProgressDialog()
                    Log.e(fragment.javaClass.simpleName, "Error al obtener artículos de Dashboard", e)
                }
    }

}