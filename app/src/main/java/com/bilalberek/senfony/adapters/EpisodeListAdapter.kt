package com.bilalberek.senfony.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bilalberek.senfony.databinding.EpisodeItemBinding
import com.bilalberek.senfony.utility.DateUtil
import com.bilalberek.senfony.utility.HtmlUtils
import com.bilalberek.senfony.utility.Utility
import com.bilalberek.senfony.viewModel.PodcastViewModel
import com.bilalberek.senfony.viewModel.SearchViewModel

class EpisodeListAdapter(): RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {

    inner class ViewHolder(binding: EpisodeItemBinding): RecyclerView.ViewHolder(binding.root){
            val tittleTextView: TextView = binding.titleView
            val descTextView: TextView = binding.descView
            val durationTextView: TextView = binding.durationView
            val releaseDateTextView : TextView = binding.releaseDateView
    }

    private val differCallback = object :
        DiffUtil.ItemCallback<PodcastViewModel.EpisodeViewData>() {
        override fun areItemsTheSame(oldItem:
                                     PodcastViewModel.EpisodeViewData, newItem:
                                     PodcastViewModel.EpisodeViewData): Boolean {
            return oldItem.guid == newItem.guid
        }

        override fun areContentsTheSame(oldItem: PodcastViewModel.EpisodeViewData,
                                        newItem: PodcastViewModel.EpisodeViewData)
                : Boolean {
            return oldItem == newItem
        }


    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(EpisodeItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       val  episodeView = differ.currentList[position]
        holder.apply {
            tittleTextView.text = episodeView.title
            descTextView.text = HtmlUtils.htmlToSpannable(episodeView.description ?: "")
            durationTextView.text = episodeView.duration
            releaseDateTextView.text = episodeView.releaseDate?.let { date ->
                DateUtil.dateToShortDate(date)
            }
        }

    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}