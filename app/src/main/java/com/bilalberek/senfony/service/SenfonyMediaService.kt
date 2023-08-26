package com.bilalberek.senfony.service

import android.media.session.MediaSession
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat

class SenfonyMediaService: MediaBrowserServiceCompat() {
     private lateinit var mediaSession: MediaSessionCompat


    override fun onCreate() {
        println("MediaService oncreate called")
        super.onCreate()
        createMediaSession()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        println("MediaService onGetRoot called")
        return BrowserRoot(SEARCH_SERVICE,null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        println("MediaService onChildren called")
        if(parentId == SENFONY_EMPTY_ROOT_MEDIA_ID){
            result.sendResult(null)
        }
    }

    private fun createMediaSession(){
        println("MediaService createMediaSession called")
        mediaSession = MediaSessionCompat(this,"SenfonyMediaService")
        sessionToken = mediaSession.sessionToken

        val callback = SenfonyMediaCallback(this, mediaSession)
        mediaSession.setCallback(callback)
    }

    companion object {
        private const val SENFONY_EMPTY_ROOT_MEDIA_ID =
            "senfony_empty_root_media_id"
    }
}