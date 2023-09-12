package com.bilalberek.senfony.ui.fragments

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bilalberek.senfony.databinding.FragmentEpisodePlayerBinding
import com.bilalberek.senfony.service.SenfonyMediaService
import com.bilalberek.senfony.utility.HtmlUtils
import com.bilalberek.senfony.viewModel.PodcastViewModel
import com.bumptech.glide.Glide

class EpisodePlayerFragment : Fragment() {

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private val podcastViewModel: PodcastViewModel by activityViewModels()
 private lateinit var binding: FragmentEpisodePlayerBinding


    inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
        }
    }

    inner class MediaControllerCallback:MediaControllerCompat.Callback(){
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {

            super.onMetadataChanged(metadata)
            println("metaData changed to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("state changed to $state")
            val _state = state ?: return
            handleStateChange(_state.getState())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMediaBrowser()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupControls()
        updateControls()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEpisodePlayerBinding.inflate(inflater,container,false)
        return binding.root
    }


    private fun updateControls() {
        println("episodePlayerFragment updateControls() called ")

        binding.episodeTitleTextView.text =
            podcastViewModel.activeEpisodeViewData?.title

        val htmlDesc =
            podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
        binding.episodeDescTextView.text = descSpan
        binding.episodeDescTextView.movementMethod =
            ScrollingMovementMethod()

        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity)
            .load(podcastViewModel.podcastLiveData.value?.imgUrl)
            .into(binding.episodeImageView)
    }

    private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData){
        println("EpisodePlayerFragment startPlaying called")
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val viewData = podcastViewModel.activePodcast ?: return
        val bundle = Bundle()
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_TITLE,
            episodeViewData.title)
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_ARTIST,
            viewData.feedTitle)
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
            viewData.imageUrl)
        controller.transportControls.playFromUri(
            Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    private fun initMediaBrowser(){
        println("EpisodePlayerFragment initMediaBrowserCalled")
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(fragmentActivity, ComponentName(fragmentActivity,
            SenfonyMediaService::class.java),
            MediaBrowserCallBacks(),null)
    }

    private fun registerMediaController(token: MediaSessionCompat.Token){
        println("episodePlayerFragment registerMediaController called")
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity,token)

        MediaControllerCompat.setMediaController(fragmentActivity,mediaController)

        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    companion object {
        fun newInstance(): EpisodePlayerFragment{
            return EpisodePlayerFragment()
        }
    }

    override fun onStart() {
        super.onStart()

        println("episodePlayerFragment onStart called")

        if (mediaBrowser.isConnected){
            val fragmentActivity = activity as FragmentActivity
            if(MediaControllerCompat.getMediaController(fragmentActivity) == null){
                registerMediaController(mediaBrowser.sessionToken)
            }
        }else{
            mediaBrowser.connect()
            println("episodePlayerFragment initMediaBrowserCalled try to connect mediaBrowser")
        }
    }

    override fun onStop() {
        super.onStop()
        println("episodePlayer onStop called" )
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity).unregisterCallback(it)
            }
        }
    }


    private fun togglePlayPause(){
        var fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat
            .getMediaController(fragmentActivity)
        if (controller.playbackState != null){
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING){
                controller.transportControls.pause()
                println("episodePlayerFragment togglePlayPause pausee")
            }else{
                println("episodePlayerFragment togglePlayPause playy")
                podcastViewModel.activeEpisodeViewData?.let {
                    startPlaying(it)
                }
            }
        }else{
            println("episodePlayerFragment togglePlayPause playy")
            podcastViewModel.activeEpisodeViewData?.let{
                startPlaying(it)
            }
        }

    }

    private fun setupControls(){
        println("episodePlayerFragment setupControls")
        binding.playToggleButton.setOnClickListener {
            togglePlayPause()
        }
    }

    private fun handleStateChange(state: Int){
        println("episodePlayerFragment handleStateChange")
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        binding.playToggleButton.isActivated = isPlaying
    }


}