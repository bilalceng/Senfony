package com.bilalberek.senfony.ui.fragments

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.media.MediaBrowserServiceCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bilalberek.senfony.R
import com.bilalberek.senfony.adapters.EpisodeListAdapter
import com.bilalberek.senfony.adapters.EpisodeListAdapterListener
import com.bilalberek.senfony.databinding.FragmentPodcastDetailsBinding
import com.bilalberek.senfony.service.SenfonyMediaService
import com.bilalberek.senfony.utility.Utility
import com.bilalberek.senfony.viewModel.PodcastViewModel
import com.bumptech.glide.Glide


class PodcastDetailsFragment : Fragment(),EpisodeListAdapterListener {
    private lateinit var listener: OnDetailsFragmentListener
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var binding: FragmentPodcastDetailsBinding
//media staff--------

    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        listener.onShowEpisodePlayer(episodeViewData)

    }
//media staff---------


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)


            setHasOptionsMenu(true)

        }


        override fun onAttach(context: Context) {
            super.onAttach(context)

            if (context is OnDetailsFragmentListener) {
                listener = context
            } else {
                throw RuntimeException(context.toString() + "must implement OnPodcastDatailListener")
            }
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.menu_feed_action -> {
                    if (item.title == getString(R.string.unsubscribe)) {

                        listener?.onUnSubscribe()
                    } else {
                        listener?.onSubscribe()
                    }
                    true
                }
                else ->
                    super.onOptionsItemSelected(item)
            }
        }


        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            // Inflate the layout for this fragment

            binding = FragmentPodcastDetailsBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)

            inflater.inflate(R.menu.menu_details, menu)
        }


        override fun onPrepareOptionsMenu(menu: Menu) {
            podcastViewModel.podcastLiveData.observe(viewLifecycleOwner) { podcast ->
                if (podcast != null) {
                    Log.d("yarrrak", "${podcast.subscribed}")
                    menu.findItem(R.id.menu_feed_action).title =
                        if (podcast.subscribed) getString(R.string.unsubscribe) else
                            getString(R.string.subscribe)
                }
            }

            super.onPrepareOptionsMenu(menu)
        }


        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            updateControls()
        }

        fun updateControls() {
            podcastViewModel.podcastLiveData.observe(
                viewLifecycleOwner
            ) { viewData ->
                if (viewData != null) {
                    binding.feedTitleTextView.text = viewData.feedTitle
                    binding.feedDescTextView.text = viewData.feedDesc
                    activity?.let { activity ->
                        Glide.with(activity).load(viewData.imgUrl).into(binding.feedImageView)
                    }

                    binding.feedDescTextView.movementMethod =
                        ScrollingMovementMethod()

                    binding.episodeRecyclerView.setHasFixedSize(true)
                    val layoutManager = LinearLayoutManager(activity)
                    binding.episodeRecyclerView.layoutManager = layoutManager
                    val dividerItemDecoration = DividerItemDecoration(
                        binding.episodeRecyclerView.context,
                        layoutManager.orientation
                    )
                    binding.episodeRecyclerView.addItemDecoration(dividerItemDecoration)

                    episodeListAdapter = EpisodeListAdapter(this)
                    episodeListAdapter.differ.submitList(viewData.episode)
                    binding.episodeRecyclerView.adapter = episodeListAdapter
                    activity?.invalidateOptionsMenu()
                }

            }

        }

        companion object {
            fun newInstance(): PodcastDetailsFragment {
                return PodcastDetailsFragment()
            }
        }


        interface OnDetailsFragmentListener {
            fun onSubscribe()
            fun onUnSubscribe()
            fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
        }

    override fun onStop() {
        super.onStop()
        println("PodcastDetailFragment  onStop called")
    }

}
