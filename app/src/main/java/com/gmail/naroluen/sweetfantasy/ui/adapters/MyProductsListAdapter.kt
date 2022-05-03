package com.gmail.naroluen.sweetfantasy.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gmail.naroluen.sweetfantasy.R
import com.gmail.naroluen.sweetfantasy.model.Product
import com.gmail.naroluen.sweetfantasy.utils.GlideLoader
import kotlinx.android.synthetic.main.item_list_layout.view.*

open class MyProductsListAdapter(
        private val context: Context,
        private var list: ArrayList<Product>,
        //private val fragment: ProductsFragment
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
                LayoutInflater.from(context).inflate(
                        R.layout.item_list_layout,
                        parent,
                        false
                )
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {

            GlideLoader(context).loadProductPicture(model.image, holder.itemView.iv_item_image)

            holder.itemView.tv_item_name.text = model.title
            holder.itemView.tv_item_price.text = "$${model.price}"
            /*
            // TODO Step 4: Assigning the click event to the delete button.
            // START
            holder.itemView.ib_delete_product.setOnClickListener {

                // TODO Step 8: Now let's call the delete function of the ProductsFragment.
                // START
                fragment.deleteProduct(model.product_id)
                // END */
            }
            // END
        }

    override fun getItemCount(): Int {
        return list.size
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

