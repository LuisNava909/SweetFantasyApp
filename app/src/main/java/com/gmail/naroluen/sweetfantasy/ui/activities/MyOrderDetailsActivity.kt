package com.gmail.naroluen.sweetfantasy.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.gmail.naroluen.sweetfantasy.R
import com.gmail.naroluen.sweetfantasy.model.Order
import com.gmail.naroluen.sweetfantasy.utils.Constants
import kotlinx.android.synthetic.main.activity_my_order_details.*

class MyOrderDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_order_details)
        setupActionBar()

        //Get the order details through intent.
        val myOrderDetails: Order
        if (intent.hasExtra(Constants.EXTRA_MY_ORDER_DETAILS)) {
            myOrderDetails = intent.getParcelableExtra<Order>(Constants.EXTRA_MY_ORDER_DETAILS)!!
        }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar_my_order_details_activity)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }
        toolbar_my_order_details_activity.setNavigationOnClickListener { onBackPressed() }
    }

}