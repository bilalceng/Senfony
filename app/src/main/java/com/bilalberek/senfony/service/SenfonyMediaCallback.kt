package com.bilalberek.senfony.service

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
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
    private var mediaNeedsPrepare: Boolean = false
    var listener: SenfonyMediaListener? = null
    private var focusRequest: AudioFocusRequest? = null
    private var mediaUri:Uri? = null
    private var newMedia:Boolean = false
    private var mediaExtras: Bundle? = null


    interface SenfonyMediaListener {
        fun onStateChanged()
        fun onStopPlaying()
        fun onPausePlaying()
        fun skipToNext()
    }


    private fun setState(state: Int, newSpeed: Float? = null)
    {
        var position: Long = -1

        mediaPlayer?.let {
           position =  it.currentPosition.toLong()
            println("current position: $position")
        }

            var speed = 1.0f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(newSpeed == null){
                    println(newSpeed)
                    speed = mediaPlayer?.playbackParams?.speed ?: 1.0f
                }else{
                    println(newSpeed)
                    speed = newSpeed
                }

                mediaPlayer?.let{ mediaPlayer->
                    try {
                        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
                    }catch (e: Exception){
                        mediaPlayer.reset()
                        mediaUri?.let { mediaUri ->
                            mediaPlayer.setDataSource(context , mediaUri)
                        }
                        mediaPlayer.prepare()
                        mediaPlayer.playbackParams =  mediaPlayer.playbackParams.setSpeed(speed)
                        mediaPlayer.seekTo(position.toInt())
                        if (state == PlaybackStateCompat.STATE_PLAYING){
                            mediaPlayer.start()
                        }
                    }
                }

            }

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PAUSE
            ).setState(state,position,speed)
            .build()

        mediaSession.setPlaybackState(playbackState)

        if (state == PlaybackStateCompat.STATE_PAUSED ||
            state == PlaybackStateCompat.STATE_PLAYING) {
            println("setState  if (state == PlaybackStateCompat.STATE_PAUSED ||\n" +
                    "            state == PlaybackStateCompat.STATE_PLAYING) ")
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
            println("onPlayFromUri new Uri")
            mediaExtras = extras
            setNewMedia(uri)
        }
        onPlay()

    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        mediaPlayer?.seekTo(pos.toInt())

        val playbackState:PlaybackStateCompat? = mediaSession.controller.playbackState

        if(playbackState != null){
            setState(playbackState.state)

        }else{
            setState(PlaybackStateCompat.STATE_PAUSED)
        }

    }

    private fun changeSpeed(extras: Bundle){
        var playbackState = PlaybackStateCompat.STATE_PAUSED
        if(mediaSession.controller.playbackState != null){
            playbackState = mediaSession.controller.playbackState.state
        }
        setState(playbackState,extras.getFloat(CMD_EXTRA_SPEED))
    }

    private fun setNewMedia(uri: Uri){
        println("onPlayFromUri new Uri setNewUri")
        mediaUri = uri
        newMedia = true
    }

    override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
        super.onCommand(command, extras, cb)
        println("onCommand out when")
        when(command){

            CMD_CHANGE_SPEED -> extras?.let { changeSpeed(it)
            println("onCommand in when")
            }
        }
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
     println("initializeMediaPlayer()")
     if (mediaPlayer == null){
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setOnCompletionListener{
                println("completed initializeMediaPlayer")
                setState(PlaybackStateCompat.STATE_PAUSED)

            }
         mediaNeedsPrepare = true
     }
 }

    private fun prepareMedia() {
        println("private fun prepareMedia()")
        if (newMedia) {
            newMedia = false
            mediaPlayer?.let { mediaPlayer ->
                mediaUri?.let { mediaUri ->
                    if(mediaNeedsPrepare) {
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(context, mediaUri)
                        mediaPlayer.prepare()
                    }
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
                                ).putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                    mediaPlayer.duration.toLong())
                                .build()
                        )

                    }

                }
            }
        }
    }

    private fun startPlaying(){
        println("startPlaying()")
        mediaPlayer?.let { mediaPlayer ->
            if(!mediaPlayer.isPlaying){
                mediaPlayer.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun pausePlaying(){
        removeAudioFocus()
        println("pausePlaying()")
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
        println("stopPlaying()")
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
            println("override fun onPlay()")
            initializeMediaPlayer()
            prepareMedia()
            startPlaying()
        }

    }

    override fun onStop() {
        super.onStop()
        println("override fun onStop()")
       stopPlaying()
    }

    override fun onPause() {
        super.onPause()
        println(" override fun onPause()")
        pausePlaying()

    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        println("bilal")
        if (ensureAudioFocus()) {
            mediaSession.isActive = true
            println("override fun skipToNext()")
            initializeMediaPlayer()
            prepareMedia()
            startPlaying()
            listener?.skipToNext()
        }
    }


    companion object{
        const val CMD_CHANGE_SPEED = "change_speed"
        const val CMD_EXTRA_SPEED = "speed"
    }
}