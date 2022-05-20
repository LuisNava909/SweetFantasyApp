package com.gmail.naroluen.sweetfantasy.ui.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gmail.naroluen.sweetfantasy.R
import com.gmail.naroluen.sweetfantasy.firestore.FirestoreClass
import com.gmail.naroluen.sweetfantasy.model.Address
import com.gmail.naroluen.sweetfantasy.ui.adapters.AddressListAdapter
import com.gmail.naroluen.sweetfantasy.utils.Constants
import com.gmail.naroluen.sweetfantasy.utils.SwipeToDeleteCallback
import com.gmail.naroluen.sweetfantasy.utils.SwipeToEditCallback
import kotlinx.android.synthetic.main.activity_address_list.*

class AddressListActivity : BaseActivity() {

    private var mSelectAddress: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_address_list)
        setupActionBar()

        tv_add_address.setOnClickListener {
            val intent = Intent(this@AddressListActivity, AddEditAddressActivity::class.java)
            //startActivity(intent)
            //Now to notify the address list about the latest address added we need to make neccessary changes as below.
            startActivityForResult(intent, Constants.ADD_ADDRESS_REQUEST_CODE)
        }
        getAddressList()
        if (intent.hasExtra(Constants.EXTRA_SELECT_ADDRESS)) {
            mSelectAddress = intent.getBooleanExtra(Constants.EXTRA_SELECT_ADDRESS, false)
        }

        if (mSelectAddress) {
            tv_title.text = resources.getString(R.string.title_select_address)
        }

    }

    // Override the onActivityResult function and get the latest address list based on the result code.
    // START
    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(Intent, int)}.  This follows the
     * related Activity API as described there in
     * {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.ADD_ADDRESS_REQUEST_CODE) {
                getAddressList()
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // A log is printed when user close or cancel the image selection.
            Log.e("Request Cancelled", "To add the address.")
        }
    }

    //Get the list of address from cloud firestore.
    private fun getAddressList() {
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAddressesList(this@AddressListActivity)
    }

    /**
     * A function to get the success result of address list from cloud firestore.
     *
     * @param addressList
     */
    fun successAddressListFromFirestore(addressList: ArrayList<Address>) {
        hideProgressDialog()
        //Populate the address list in the UI.
        if (addressList.size > 0) {
            rv_address_list.visibility = View.VISIBLE
            tv_no_address_found.visibility = View.GONE
            rv_address_list.layoutManager = LinearLayoutManager(this@AddressListActivity)
            rv_address_list.setHasFixedSize(true)
            val addressAdapter = AddressListAdapter(this@AddressListActivity, addressList, mSelectAddress)
            rv_address_list.adapter = addressAdapter

            if (!mSelectAddress) {
                val editSwipeHandler = object : SwipeToEditCallback(this) {
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val adapter = rv_address_list.adapter as AddressListAdapter
                        adapter.notifyEditItem(
                                this@AddressListActivity,
                                viewHolder.adapterPosition
                        )
                    }
                }
                val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
                editItemTouchHelper.attachToRecyclerView(rv_address_list)

                val deleteSwipeHandler = object : SwipeToDeleteCallback(this) {
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        showProgressDialog(resources.getString(R.string.please_wait))
                        FirestoreClass().deleteAddress(
                                this@AddressListActivity,
                                addressList[viewHolder.adapterPosition].id
                        )
                    }
                }
                val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
                deleteItemTouchHelper.attachToRecyclerView(rv_address_list)
            }
        } else {
            rv_address_list.visibility = View.GONE
            tv_no_address_found.visibility = View.VISIBLE
        }
    }

    fun deleteAddressSuccess() {
        hideProgressDialog()
        Toast.makeText(this@AddressListActivity, resources.getString(R.string.err_your_address_deleted_successfully), Toast.LENGTH_SHORT).show()
        getAddressList()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_address_list_activity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }
        toolbar_address_list_activity.setNavigationOnClickListener { onBackPressed() }
    }
}