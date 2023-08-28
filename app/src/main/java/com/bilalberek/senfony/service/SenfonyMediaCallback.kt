package com.bilalberek.senfony.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

/*
Interaction with MediaSession: Once a media item is selected, the MediaBrowser
 can use the associated MediaSession to control playback through a MediaController.
 */

class SenfonyMediaCallback(val context: Context,
                           private val mediaSession: MediaSessionCompat,
                           var mediaPlayer: MediaPlayer? = null
):MediaSessionCompat.Callback() {
    var listener: SenfonyMediaListener? = null
    private var focusRequest: AudioFocusRequest? = null
    private var mediaUri:Uri? = null
    private var newMedia:Boolean = false
    private var mediaExtras: Bundle? = null


    interface SenfonyMediaListener {
        fun onStateChanged()
        fun onStopPlaying()
        fun onPausePlaying()
    }

    private fun setState(state: Int)
    {
        var position: Long = -1

        mediaPlayer?.let {
           position =  it.currentPosition.toLong()
        }
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PAUSE
            ).setState(state,position,1.0f)
            .build()

        mediaSession.setPlaybackState(playbackState)

        if (state == PlaybackStateCompat.STATE_PAUSED ||
            state == PlaybackStateCompat.STATE_PLAYING) {
            listener?.onStateChanged()
        }
    }

    override fun onPlayFromUri(uri: Uri, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        println("Playing ${uri.toString()}")
        if (mediaUri == uri) {
            newMedia = false
            mediaExtras = null
        } else {
            mediaExtras = extras
            setNewMedia(uri)
        }
        onPlay()

    }

    private fun setNewMedia(uri: Uri){
        mediaUri = uri
        newMedia = true
    }

    private fun ensureAudioFocus(): Boolean{
        var audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()

                })
                build()
            }

            this.focusRequest = focusRequest
            val result = audioManager.requestAudioFocus(focusRequest)

            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            val result = audioManager.requestAudioFocus(null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN)
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun removeAudioFocus(){
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusRequest?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(it)
            } else {
               audioManager.abandonAudioFocus(null)
            }
        }
    }

 private fun initializeMediaPlayer(){
     if (mediaPlayer == null){
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnCompletionListener{
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
     }
 }

    private fun prepareMedia() {
        if (newMedia) {
            newMedia = false
            mediaPlayer?.let { mediaPlayer ->
                mediaUri?.let { mediaUri ->
                    mediaPlayer.reset()
                    mediaPlayer.setDataSource(context, mediaUri)
                    mediaPlayer.prepare()
                    mediaExtras?.let { mediaExtras ->
                        mediaSession.setMetadata(
                            MediaMetadataCompat.Builder()
                                .putString(
                                    MediaMetadataCompat.METADATA_KEY_TITLE,
                                    mediaExtras.getString(
                                        MediaMetadataCompat.METADATA_KEY_TITLE
                                    )
                                )
                                .putString(
                                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                                    mediaExtras.getString(
                                        MediaMetadataCompat.METADATA_KEY_ARTIST
                                    )
                                )
                                .putString(
                                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                    mediaExtras.getString(
                                        MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI
                                    )
                                )
                                .build()
                        )

                    }

                }
            }
        }
    }

    private fun startPlaying(){
        mediaPlayer?.let { mediaPlayer ->
            if(!mediaPlayer.isPlaying){
                mediaPlayer.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun pausePlaying(){
        removeAudioFocus()
        mediaPlayer?.let { mediaPlayer ->
            if(mediaPlayer.isPlaying){
                mediaPlayer.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
        listener?.onPausePlaying()
    }

    private fun stopPlaying(){
        removeAudioFocus()
        mediaSession.isActive = false
        mediaPlayer?.let{ mediaPlayer ->
            if (mediaPlayer.isPlaying){
                mediaPlayer.stop()
                setState(PlaybackStateCompat.STATE_STOPPED)
            }
        }

        listener?.onStopPlaying()
    }

    override fun onPlay() {
        super.onPlay()
        if (ensureAudioFocus()){
            mediaSession.isActive = true
            println("onPlay called")
            initializeMediaPlayer()
            prepareMedia()
            startPlaying()
        }

    }

    override fun onStop() {
        super.onStop()
       stopPlaying()
    }

    override fun onPause() {
        super.onPause()
        println("onPause called")
        pausePlaying()

    }
}