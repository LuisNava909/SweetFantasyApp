package com.gmail.naroluen.sweetfantasy.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gmail.naroluen.sweetfantasy.R
import com.gmail.naroluen.sweetfantasy.firestore.FirestoreClass
import com.gmail.naroluen.sweetfantasy.model.Product
import com.gmail.naroluen.sweetfantasy.ui.activities.AddProductActivity
import com.gmail.naroluen.sweetfantasy.ui.adapters.MyProductsListAdapter
import kotlinx.android.synthetic.main.fragment_products.*

class ProductsFragment : BaseFragment() {

    //private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * A function to get the successful product list from cloud firestore.
     *
     * @param productsList Will receive the product list from cloud firestore.
     */
    fun successProductsListFromFireStore(productsList: ArrayList<Product>) {
        // Hide Progress dialog.
        hideProgressDialog()

        if (productsList.size > 0) {
            rv_my_product_items.visibility = View.VISIBLE
            tv_no_products_found.visibility = View.GONE

            rv_my_product_items.layoutManager = LinearLayoutManager(activity)
            rv_my_product_items.setHasFixedSize(true)

            // TODO Step 7: Pass the third parameter value.
            // START
            val adapterProducts =
                    MyProductsListAdapter(requireActivity(), productsList)
            // END
            rv_my_product_items.adapter = adapterProducts
        } else {
            rv_my_product_items.visibility = View.GONE
            tv_no_products_found.visibility = View.VISIBLE
        }
    }

    private fun getProductListFromFireStore() {
        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        // Call the function of Firestore class.
        FirestoreClass().getProductsList(this@ProductsFragment)
    }

    override fun onResume() {
        super.onResume()
        getProductListFromFireStore()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_products, container, false)

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_product_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_add_product) {
            startActivity(Intent(activity, AddProductActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}