package com.gmail.naroluen.sweetfantasy.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.gmail.naroluen.sweetfantasy.R
import com.gmail.naroluen.sweetfantasy.firestore.FirestoreClass
import com.gmail.naroluen.sweetfantasy.model.CartItem
import com.gmail.naroluen.sweetfantasy.model.Product
import com.gmail.naroluen.sweetfantasy.ui.adapters.CartItemsListAdapter
import com.gmail.naroluen.sweetfantasy.utils.Constants
import kotlinx.android.synthetic.main.activity_cart_list.*


class CartListActivity : BaseActivity() {
    // A global variable for the product list.
    private lateinit var mProductsList: ArrayList<Product>
    // A global variable for the cart list items.
    private lateinit var mCartListItems: ArrayList<CartItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart_list)
        setupActionBar()

        //Assign the click event to the checkout button and proceed to the next screen.
        btn_checkout.setOnClickListener {
            val intent = Intent(this@CartListActivity, AddressListActivity::class.java)
            intent.putExtra(Constants.EXTRA_SELECT_ADDRESS, true)
            startActivity(intent)
        }

    }

    fun successCartItemsList(cartList: ArrayList<CartItem>) {
        hideProgressDialog()
        //Compare the product id of product list with product id of cart items list and update the stock quantity in the cart items list from the latest product list.
        for (product in mProductsList) {
            for (cartItem in cartList) {
                if (product.product_id == cartItem.product_id) {

                    cartItem.stock_quantity = product.stock_quantity

                    if (product.stock_quantity.toInt() == 0){
                        cartItem.cart_quantity = product.stock_quantity
                    }
                }
            }
        }
        mCartListItems = cartList
        /*for (i in cartList) {
            Log.i("Cart Item Title", i.title)
        }*/
        if (mCartListItems.size > 0) {
            rv_cart_items_list.visibility = View.VISIBLE
            ll_checkout.visibility = View.VISIBLE
            tv_no_cart_item_found.visibility = View.GONE

            rv_cart_items_list.layoutManager = LinearLayoutManager(this@CartListActivity)
            rv_cart_items_list.setHasFixedSize(true)

            val cartListAdapter = CartItemsListAdapter(this@CartListActivity, cartList)
            rv_cart_items_list.adapter = cartListAdapter

            var subTotal: Double = 0.0

            for (item in mCartListItems) {
                //Calculate the subtotal based on the stock quantity.
                val availableQuantity = item.stock_quantity.toInt()
                if (availableQuantity > 0) {
                    val price = item.price.toDouble()
                    val quantity = item.cart_quantity.toInt()
                    subTotal += (price * quantity)
                }
            }
            tv_sub_total.text = "$$subTotal"
            // Here we have kept Shipping Charge is fixed as $10 but in your case it may cary. Also, it depends on the location and total amount.
            tv_shipping_charge.text = "$100.0"

            if (subTotal > 0) {
                ll_checkout.visibility = View.VISIBLE
                val total = subTotal + 100
                tv_total_amount.text = "$$total"
            } else {
                ll_checkout.visibility = View.GONE
            }
        } else {
            rv_cart_items_list.visibility = View.GONE
            ll_checkout.visibility = View.GONE
            tv_no_cart_item_found.visibility = View.VISIBLE
        }
    }

    /**
     * Get the success result of product list.
     *
     * @param productsList
     */
    fun successProductsListFromFireStore(productsList: ArrayList<Product>) {
        hideProgressDialog()
        //Initialize the product list global variable once we have the product list.
        mProductsList = productsList
        //Once we have the latest product list from cloud firestore get the cart items list from cloud firestore.
        getCartItemsList()
    }

    /**
     * Get product list to compare the current stock with the cart items.
     */
    private fun getProductList() {
        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAllProductsList(this@CartListActivity)
    }

    private fun getCartItemsList() {
        // Show the progress dialog.
        //showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getCartList(this@CartListActivity)
    }

    //Function to notify the user about the item quantity updated in the cart list.
    fun itemUpdateSuccess() {
        hideProgressDialog()
        getCartItemsList()
    }

    //Override the onResume function and call the function to getCartItemsList.
    override fun onResume() {
        super.onResume()
        //getCartItemsList()
        getProductList()
    }

    fun itemRemovedSuccess() {
        hideProgressDialog()
        Toast.makeText(
                this@CartListActivity,
                resources.getString(R.string.msg_item_removed_successfully),
                Toast.LENGTH_SHORT
        ).show()
        getCartItemsList()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_cart_list_activity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24)
        }
        toolbar_cart_list_activity.setNavigationOnClickListener { onBackPressed() }
    }
}