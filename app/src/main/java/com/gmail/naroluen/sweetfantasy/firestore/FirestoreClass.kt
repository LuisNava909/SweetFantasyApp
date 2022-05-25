package com.gmail.naroluen.sweetfantasy.firestore

import android.app.Activity
import androidx.fragment.app.Fragment
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.gmail.naroluen.sweetfantasy.model.*
import com.gmail.naroluen.sweetfantasy.ui.activities.*
import com.gmail.naroluen.sweetfantasy.ui.fragments.DashboardFragment
import com.gmail.naroluen.sweetfantasy.ui.fragments.OrdersFragment
import com.gmail.naroluen.sweetfantasy.ui.fragments.ProductsFragment
import com.gmail.naroluen.sweetfantasy.ui.fragments.SoldProductsFragment
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
     * A function to get the product details based on the product id.
     */
    fun getProductDetails(activity: ProductDetailsActivity, productId: String) {
        // The collection name for PRODUCTS
        mFireStore.collection(Constants.PRODUCTS)
            .document(productId)
            .get() // Will get the document snapshots.
            .addOnSuccessListener { document ->
                // Here we get the product details in the form of document.
                Log.e(activity.javaClass.simpleName, document.toString())
                // Convert the snapshot to the object of Product data model class.
                val product = document.toObject(Product::class.java)!!

                activity.productDetailsSuccess(product)
            }
            .addOnFailureListener { e ->
                // Hide the progress dialog if there is an error.
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error detalles de artículo", e)
            }
    }

    /**
     * A function to add the item to the cart in the cloud firestore.
     *
     * @param activity
     * @param addToCart
     */
    fun addCartItems(activity: ProductDetailsActivity, addToCart: CartItem) {
        mFireStore.collection(Constants.CART_ITEMS)
                .document()
                // Here the userInfo are Field and the SetOption is set to merge. It is for if we wants to merge
                .set(addToCart, SetOptions.merge())
                .addOnSuccessListener {
                    // Here call a function of base activity for transferring the result to it.
                    activity.addToCartSuccess()
                }
                .addOnFailureListener { e ->
                    activity.hideProgressDialog()
                    Log.e(
                            activity.javaClass.simpleName,
                            "Error while creating the document for cart item.",
                            e
                    )
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

    fun getCartList(activity: Activity) {
        // The collection name for PRODUCTS
        mFireStore.collection(Constants.CART_ITEMS)
            .whereEqualTo(Constants.USER_ID, getCurrentUserID())
            .get() // Will get the documents snapshots.
            .addOnSuccessListener { document ->
                // Here we get the list of cart items in the form of documents.
                Log.e(activity.javaClass.simpleName, document.documents.toString())
                // Here we have created a new instance for Cart Items ArrayList.
                val list: ArrayList<CartItem> = ArrayList()
                // A for loop as per the list of documents to convert them into Cart Items ArrayList.
                for (i in document.documents) {
                    val cartItem = i.toObject(CartItem::class.java)!!
                    cartItem.id = i.id
                    list.add(cartItem)
                }
                //Notify the success result.
                when (activity) {
                    is CartListActivity -> {
                        activity.successCartItemsList(list)
                    }
                    is CheckoutActivity -> {
                        activity.successCartItemsList(list)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Hide the progress dialog if there is an error based on the activity instance.
                when (activity) {
                    is CartListActivity -> {
                        activity.hideProgressDialog()
                    }
                    is CheckoutActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Log.e(activity.javaClass.simpleName, "Error while getting the cart list items.", e)
            }
    }

    /**
     * A function to update all the required details in the cloud firestore after placing the order successfully.
     *
     * @param activity Base class.
     * @param cartList List of cart items.
     */
    fun updateAllDetails(activity: CheckoutActivity, cartList: ArrayList<CartItem>, order:Order) {
        val writeBatch = mFireStore.batch()
        //Update the product stock in the products collection based to cart quantity.
        for (cartItem in cartList) {
            //val productHashMap = HashMap<String, Any>()
            //productHashMap[Constants.STOCK_QUANTITY] = (cartItem.stock_quantity.toInt() - cartItem.cart_quantity.toInt()).toString()

            val soldProduct = SoldProduct(
                    // Here the user id will be of product owner.
                    cartItem.product_owner_id,
                    cartItem.title,
                    cartItem.price,
                    cartItem.cart_quantity,
                    cartItem.image,
                    order.title,
                    order.order_datetime,
                    order.sub_total_amount,
                    order.shipping_charge,
                    order.total_amount,
                    order.address
            )

            val documentReference = mFireStore.collection(Constants.SOLD_PRODUCTS).document()
            writeBatch.set(documentReference, soldProduct)
        }
        // Delete the list of cart items
        for (cartItem in cartList) {

            val documentReference = mFireStore.collection(Constants.CART_ITEMS)
                    .document(cartItem.id)
            writeBatch.delete(documentReference)
        }
        writeBatch.commit().addOnSuccessListener {
            //Notify the user with the success result.
            activity.allDetailsUpdatedSuccessfully()
        }.addOnFailureListener { e ->
            // Here call a function of base activity for transferring the result to it.
            activity.hideProgressDialog()

            Log.e(activity.javaClass.simpleName, "Error while updating all the details after order placed.", e)
        }
    }

    /**
     * A function to get the list of sold products from the cloud firestore.
     *
     *  @param fragment Base class
     */
    fun getSoldProductsList(fragment: SoldProductsFragment) {
        // The collection name for SOLD PRODUCTS
        mFireStore.collection(Constants.SOLD_PRODUCTS)
                .whereEqualTo(Constants.USER_ID, getCurrentUserID())
                .get() // Will get the documents snapshots.
                .addOnSuccessListener { document ->
                    // Here we get the list of sold products in the form of documents.
                    Log.e(fragment.javaClass.simpleName, document.documents.toString())

                    // Here we have created a new instance for Sold Products ArrayList.
                    val list: ArrayList<SoldProduct> = ArrayList()

                    // A for loop as per the list of documents to convert them into Sold Products ArrayList.
                    for (i in document.documents) {

                        val soldProduct = i.toObject(SoldProduct::class.java)!!
                        soldProduct.id = i.id

                        list.add(soldProduct)
                    }
                    fragment.successSoldProductsList(list)
                }
                .addOnFailureListener { e ->
                    // Hide the progress dialog if there is any error.
                    fragment.hideProgressDialog()
                    Log.e(fragment.javaClass.simpleName, "Error while getting the list of sold products.", e)
                }
    }

    fun getMyOrdersList(fragment: OrdersFragment) {
        mFireStore.collection(Constants.ORDERS)
                .whereEqualTo(Constants.USER_ID, getCurrentUserID())
                .get() // Will get the documents snapshots.
                .addOnSuccessListener { document ->
                    Log.e(fragment.javaClass.simpleName, document.documents.toString())
                    val list: ArrayList<Order> = ArrayList()

                    for (i in document.documents) {

                        val orderItem = i.toObject(Order::class.java)!!
                        orderItem.id = i.id

                        list.add(orderItem)
                    }
                    fragment.populateOrdersListInUI(list)
                }
                .addOnFailureListener { e ->
                    // Here call a function of base activity for transferring the result to it.
                    fragment.hideProgressDialog()
                    Log.e(fragment.javaClass.simpleName, "Error while getting the orders list.", e)
                }
    }

    /**
     * A function to place an order of the user in the cloud firestore.
     *
     * @param activity base class
     * @param order Order Info
     */
    fun placeOrder(activity: CheckoutActivity, order: Order) {
        mFireStore.collection(Constants.ORDERS)
            .document()
            // Here the userInfo are Field and the SetOption is set to merge. It is for if we wants to merge
            .set(order, SetOptions.merge())
            .addOnSuccessListener {
                //Call a function of base activity for transferring the result to it.
                activity.orderPlacedSuccess()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while placing an order.",
                    e
                )
            }
    }

    /**
     * A function to delete the existing address from the cloud firestore.
     *
     * @param activity Base class
     * @param addressId existing address id
     */
    fun deleteAddress(activity: AddressListActivity, addressId: String) {
        mFireStore.collection(Constants.ADDRESSES)
                .document(addressId)
                .delete()
                .addOnSuccessListener {
                    activity.deleteAddressSuccess()
                }
                .addOnFailureListener { e ->
                    activity.hideProgressDialog()
                    Log.e(
                            activity.javaClass.simpleName,
                            "Error while deleting the address.",
                            e
                    )
                }
    }

    /**
     * A function to update the existing address to the cloud firestore.
     *
     * @param activity Base class
     * @param addressInfo Which fields are to be updated.
     * @param addressId existing address id
     */
    fun updateAddress(activity: AddEditAddressActivity, addressInfo: Address, addressId: String) {
        mFireStore.collection(Constants.ADDRESSES)
                .document(addressId)
                // Here the userInfo are Field and the SetOption is set to merge. It is for if we wants to merge
                .set(addressInfo, SetOptions.merge())
                .addOnSuccessListener {
                    // Here call a function of base activity for transferring the result to it.
                    activity.addUpdateAddressSuccess()
                }
                .addOnFailureListener { e ->
                    activity.hideProgressDialog()
                    Log.e(activity.javaClass.simpleName, "Error while updating the Address.", e)
                }
    }

    /**
     * A function to get the list of address from the cloud firestore.
     *
     * @param activity
     */
    fun getAddressesList(activity: AddressListActivity) {
        // The collection name for ADDRESSES
        mFireStore.collection(Constants.ADDRESSES)
                .whereEqualTo(Constants.USER_ID, getCurrentUserID())
                .get() // Will get the documents snapshots.
                .addOnSuccessListener { document ->
                    // Here we get the list of boards in the form of documents.
                    Log.e(activity.javaClass.simpleName, document.documents.toString())
                    // Here we have created a new instance for address ArrayList.
                    val addressList: ArrayList<Address> = ArrayList()
                    // A for loop as per the list of documents to convert them into Boards ArrayList.
                    for (i in document.documents) {
                        val address = i.toObject(Address::class.java)!!
                        address.id = i.id
                        addressList.add(address)
                    }
                    //Notify the success result to the base class.
                    activity.successAddressListFromFirestore(addressList)
                }
                .addOnFailureListener { e ->
                    // Here call a function of base activity for transferring the result to it.
                    activity.hideProgressDialog()
                    Log.e(activity.javaClass.simpleName, "Error while getting the address list.", e)
                }
    }

    /**
     * A function to add address to the cloud firestore.
     *
     * @param activity
     * @param addressInfo
     */
    fun addAddress(activity: AddEditAddressActivity, addressInfo: Address) {
        // Collection name address.
        mFireStore.collection(Constants.ADDRESSES)
            .document()
            // Here the userInfo are Field and the SetOption is set to merge. It is for if we wants to merge
            .set(addressInfo, SetOptions.merge())
            .addOnSuccessListener {
                //Notify the success result to the base class.
                // Here call a function of base activity for transferring the result to it.
                activity.addUpdateAddressSuccess()
            }
            .addOnFailureListener { e ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while adding the address.",
                    e
                )
            }
    }

    /**
     * A function to update the cart item in the cloud firestore.
     *
     * @param activity activity class.
     * @param id cart id of the item.
     * @param itemHashMap to be updated values.
     */
    fun updateMyCart(context: Context, cart_id: String, itemHashMap: HashMap<String, Any>){
        mFireStore.collection(Constants.CART_ITEMS)
                .document(cart_id) // cart id
                .update(itemHashMap) // A HashMap of fields which are to be updated.
                .addOnSuccessListener{
                    // Notify the success result of the updated cart items list to the base class.
                    when (context) {
                        is CartListActivity -> {
                            context.itemUpdateSuccess()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Hide the progress dialog if there is any error.
                    when (context) {
                        is CartListActivity -> {
                            context.hideProgressDialog()
                        }
                    }
                    Log.e(context.javaClass.simpleName, "Error while updating the cart item.", e)
                }
    }

    fun checkIfItemExistInCart(activity: ProductDetailsActivity, productId: String) {
        mFireStore.collection(Constants.CART_ITEMS)
                .whereEqualTo(Constants.USER_ID, getCurrentUserID())
                .whereEqualTo(Constants.PRODUCT_ID, productId)
                .get()
                .addOnSuccessListener { document ->
                    Log.e(activity.javaClass.simpleName, document.documents.toString())
                    // If the document size is greater than 1 it means the product is already added to the cart.
                    if (document.documents.size > 0) {
                        activity.productExistsInCart()
                    } else {
                        activity.hideProgressDialog()
                    }
                }
                .addOnFailureListener { e ->
                    // Hide the progress dialog if there is an error.
                    activity.hideProgressDialog()
                    Log.e(
                            activity.javaClass.simpleName, "Error while checking the existing cart list.", e)
                }
    }

    /**
     * Remove the cart item from the cloud firestore.
     *
     * @param activity activity class.
     * @param cart_id cart id of the item.
     */
    fun removeItemFromCart(context: Context, cart_id: String) {
        // Cart items collection name
        mFireStore.collection(Constants.CART_ITEMS)
                .document(cart_id) // cart id
                .delete()
                .addOnSuccessListener {
                    // Notify the success result of the removed cart item from the list to the base class.
                    when (context) {
                        is CartListActivity -> {
                            context.itemRemovedSuccess()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Hide the progress dialog if there is any error.
                    when (context) {
                        is CartListActivity -> {
                            context.hideProgressDialog()
                        }
                    }
                    Log.e(
                            context.javaClass.simpleName,
                            "Error while removing the item from the cart list.",
                            e
                    )
                }
    }

    /**
     * Get all the product list from the cloud firestore.
     *
     * @param activity The activity is passed as parameter to the function because it is called from activity and need to the success result.
     */
    fun getAllProductsList(activity: Activity) {
        // The collection name for PRODUCTS
        mFireStore.collection(Constants.PRODUCTS)
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
                    when (activity) {
                        is CartListActivity -> {
                            //Pass the success result of the product list to the cart list activity.
                            activity.successProductsListFromFireStore(productsList)
                        }
                        //Notify the success result to the base class.
                        is CheckoutActivity -> {
                            activity.successProductsListFromFireStore(productsList)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    when (activity) {
                        is CartListActivity -> {
                            // Hide the progress dialog if there is any error based on the base class instance.
                            activity.hideProgressDialog()
                        }
                        is CheckoutActivity -> {
                            // Hide the progress dialog if there is any error based on the base class instance.
                            activity.hideProgressDialog()
                        }
                    }
                    Log.e("Get Product List", "Error while getting all product list.", e)
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