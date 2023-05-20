package com.bilalberek.senfony.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bilalberek.senfony.databinding.SearchItemBinding
import com.bilalberek.senfony.viewModel.SearchViewModel
import com.bumptech.glide.Glide

class PodcastListAdapter(
    private val parentActivity:Activity
):RecyclerView.Adapter<PodcastListAdapter.ViewHolder>(){

    inner class ViewHolder(
       var  binding: SearchItemBinding
    ):RecyclerView.ViewHolder(binding.root){

        var podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData? = null
        val nameTextView: TextView = binding.podcastNameTextView
        val lastUpdatedTextView: TextView = binding.podcastLastUpdatedTextView
        val podcastImageView: ImageView = binding.podcastImage

    }

    private val differCallback = object :
        DiffUtil.ItemCallback<SearchViewModel.PodcastSummaryViewData>() {
        override fun areItemsTheSame(oldItem:
                                     SearchViewModel.PodcastSummaryViewData, newItem:
        SearchViewModel.PodcastSummaryViewData): Boolean {
            return oldItem.imageUrl == newItem.imageUrl
        }

        override fun areContentsTheSame(oldItem: SearchViewModel.PodcastSummaryViewData,
                                        newItem: SearchViewModel.PodcastSummaryViewData)
        : Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SearchItemBinding.
        inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       var searchView = differ.currentList[position]

        holder.apply {
            podcastSummaryViewData = searchView
            nameTextView.text = searchView.name
            lastUpdatedTextView.text = searchView.lastUpdated
        }
        holder.binding.searchItem.setOnClickListener {
            onToolClickListener?.let {
                it(searchView)
            }
        }


        Glide.with(parentActivity)
            .load(searchView.imageUrl)
            .into(holder.podcastImageView)
    }



    var onToolClickListener: ((SearchViewModel.PodcastSummaryViewData) -> Unit)? = null

    fun setOnItemClickListener(listener: (SearchViewModel.PodcastSummaryViewData) -> Unit){
        onToolClickListener = listener
    }
    override fun getItemCount(): Int {
       return differ.currentList.size
    }

}
