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


interface EpisodeListAdapterListener{
    fun onSelectedEpisode(episodeViewData:PodcastViewModel.EpisodeViewData)
}

class EpisodeListAdapter(
    private val episodeListAdapterListener: EpisodeListAdapterListener
): RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {



    inner class ViewHolder( var binding: EpisodeItemBinding,
                            episodeListAdapterListener: EpisodeListAdapterListener): RecyclerView.ViewHolder(binding.root) {




        fun bind(episodeView: PodcastViewModel.EpisodeViewData) {
            binding.titleView.text = episodeView.title
            binding.descView.text = HtmlUtils.htmlToSpannable(episodeView.description ?: "")
            binding.durationView.text = episodeView.duration
            binding.releaseDateView.text = episodeView.releaseDate?.let { date ->
                DateUtil.dateToShortDate(date)
            }

            binding.root.setOnClickListener {
                episodeListAdapterListener.onSelectedEpisode(episodeView)
                println("${episodeView.title}")
            }

        }
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

        return ViewHolder(EpisodeItemBinding.inflate(LayoutInflater.from(parent.context),parent,false),
        episodeListAdapterListener!!)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

       var  episodeView = differ.currentList[position]
        holder.bind(episodeView)

    }

    override fun getItemCount(): Int {

        return differ.currentList.size
    }
}