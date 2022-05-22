package com.gmail.naroluen.sweetfantasy.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.gmail.naroluen.sweetfantasy.R
import com.gmail.naroluen.sweetfantasy.firestore.FirestoreClass
import com.gmail.naroluen.sweetfantasy.model.Address
import com.gmail.naroluen.sweetfantasy.model.CartItem
import com.gmail.naroluen.sweetfantasy.model.Order
import com.gmail.naroluen.sweetfantasy.model.Product
import com.gmail.naroluen.sweetfantasy.ui.adapters.CartItemsListAdapter
import com.gmail.naroluen.sweetfantasy.utils.Constants
import kotlinx.android.synthetic.main.activity_checkout.*

class CheckoutActivity : BaseActivity() {

    private var mAddressDetails: Address? = null
    private lateinit var mProductsList: ArrayList<Product>
    private lateinit var mCartItemsList: ArrayList<CartItem>
    private var mSubTotal: Double = 0.0
    private var mTotalAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)
        setupActionBar()

        //Get the selected address details through intent.
        if (intent.hasExtra(Constants.EXTRA_SELECTED_ADDRESS)) {
            mAddressDetails = intent.getParcelableExtra<Address>(Constants.EXTRA_SELECTED_ADDRESS)!!
        }

        //Set the selected address details to UI that is received through intent.
        if (mAddressDetails != null) {
            tv_checkout_address_type.text = mAddressDetails?.type
            tv_checkout_full_name.text = mAddressDetails?.name
            tv_checkout_address.text = "${mAddressDetails!!.address}, ${mAddressDetails!!.zipCode}"
            tv_checkout_additional_note.text = mAddressDetails?.additionalNote

            if (mAddressDetails?.otherDetails!!.isNotEmpty()) {
                tv_checkout_other_details.text = mAddressDetails?.otherDetails
            }
            tv_checkout_mobile_number.text = mAddressDetails?.mobileNumber
        }

        btn_place_order.setOnClickListener {
            placeAnOrder()
        }

        getProductList()
    }

    fun successProductsListFromFireStore(productsList: ArrayList<Product>) {
        //global variable of all product list.
        mProductsList = productsList
        getCartItemsList()
    }

    private fun getCartItemsList() {
        FirestoreClass().getCartList(this@CheckoutActivity)
    }

    /**
     * A function to notify the success result of the cart items list from cloud firestore.
     *
     * @param cartList
     */
    fun successCartItemsList(cartList: ArrayList<CartItem>) {
        hideProgressDialog()
        //Update the stock quantity in the cart list from the product list.
        for (product in mProductsList) {
            for (cartItem in cartList) {
                if (product.product_id == cartItem.product_id) {
                    cartItem.stock_quantity = product.stock_quantity
                }
            }
        }
        mCartItemsList = cartList
        //Populate the cart items in the UI.
        rv_cart_list_items.layoutManager = LinearLayoutManager(this@CheckoutActivity)
        rv_cart_list_items.setHasFixedSize(true)

        val cartListAdapter = CartItemsListAdapter(this@CheckoutActivity, mCartItemsList, false)
        rv_cart_list_items.adapter = cartListAdapter

        //Replace the subTotal and totalAmount variables with the global variables.
        for (item in mCartItemsList) {
            val availableQuantity = item.stock_quantity.toInt()
            if (availableQuantity > 0) {
                val price = item.price.toDouble()
                val quantity = item.cart_quantity.toInt()
                mSubTotal += (price * quantity)
            }
        }
        tv_checkout_sub_total.text = "$$mSubTotal"
        // Here we have kept Shipping Charge is fixed as $10 but in your case it may cary. Also, it depends on the location and total amount.
        tv_checkout_shipping_charge.text = "$10.0"
        if (mSubTotal > 0) {
            ll_checkout_place_order.visibility = View.VISIBLE
            mTotalAmount = mSubTotal + 100.0
            tv_checkout_total_amount.text = "$$mTotalAmount"
        } else {
            ll_checkout_place_order.visibility = View.GONE
        }
    }

    private fun placeAnOrder() {
        showProgressDialog(resources.getString(R.string.please_wait))
        if(mAddressDetails != null){
            //Prepare the order details based on all the required details.
            val order = Order(
                FirestoreClass().getCurrentUserID(),
                mCartItemsList,
                mAddressDetails!!,
                "Mi regalo ${System.currentTimeMillis()}",
                mCartItemsList[0].image,
                mSubTotal.toString(),
                "100.0", // The Shipping Charge is fixed as $10 for now in our case.
                mTotalAmount.toString(),
            )
            //Call the function to place the order in the cloud firestore.
            FirestoreClass().placeOrder(this@CheckoutActivity, order)
        }
    }

    private fun getProductList() {
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAllProductsList(this@CheckoutActivity)
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_checkout_activity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }
        toolbar_checkout_activity.setNavigationOnClickListener { onBackPressed() }
    }

    fun orderPlacedSuccess() {
        hideProgressDialog()
        Toast.makeText(this@CheckoutActivity, "Tu pedido ha sido confirmado", Toast.LENGTH_SHORT)
            .show()
        val intent = Intent(this@CheckoutActivity, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}