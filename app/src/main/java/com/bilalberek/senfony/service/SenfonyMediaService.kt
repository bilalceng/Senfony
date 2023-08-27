package com.bilalberek.senfony.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.session.MediaSession
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.bilalberek.senfony.R
import com.bilalberek.senfony.ui.PodcastActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

class SenfonyMediaService: MediaBrowserServiceCompat(),SenfonyMediaCallback.SenfonyMediaListener {
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
        return BrowserRoot(SEARCH_SERVICE, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        println("MediaService onChildren called")
        if (parentId == SENFONY_EMPTY_ROOT_MEDIA_ID) {
            result.sendResult(null)
        }
    }

    private fun createMediaSession() {
        println("MediaService createMediaSession called")
        mediaSession = MediaSessionCompat(this, "SenfonyMediaService")
        sessionToken = mediaSession.sessionToken

        val callback = SenfonyMediaCallback(this, mediaSession)
        callback.listener = this
        mediaSession.setCallback(callback)
    }

    private fun getPausePlayAction(): Pair<NotificationCompat.Action, NotificationCompat.Action> {
        val pauseAction = NotificationCompat.Action(
            R.drawable.ic_baseline_pause_24, getString(R.string.pause_value),
            MediaButtonReceiver.buildMediaButtonPendingIntent(
                this,
                PlaybackStateCompat.ACTION_PAUSE
            )
        )

        val playAction = NotificationCompat.Action(
            R.drawable.ic_baseline_play_arrow_24, getString(R.string.play_value),
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
        )

        return Pair(pauseAction, playAction)

    }

    private fun isPlaying() =
        mediaSession.controller.playbackState != null &&
                mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING



    @SuppressLint("UnspecifiedImmutableFlag")
    private fun getNotificationIntent():PendingIntent{
        val openActivityIntent = Intent(this,PodcastActivity::class.java)
        openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(this@SenfonyMediaService,0,openActivityIntent,PendingIntent.FLAG_CANCEL_CURRENT)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(){
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(notificationManager.getNotificationChannel(PLAYER_CHANNEL_ID) == null){
            val channel = NotificationChannel(PLAYER_CHANNEL_ID,"player",NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

    }


    private fun createNotification(mediaDescription: MediaDescriptionCompat, bitmap: Bitmap?)
    : Notification{
        val notificationIntent = getNotificationIntent()
        val (pauseAction, playAction) = getPausePlayAction()

        val notification = NotificationCompat.Builder(
            this@SenfonyMediaService, PLAYER_CHANNEL_ID)

        notification
            .setContentTitle(mediaDescription.title)
            .setContentText(mediaDescription.subtitle)
            .setLargeIcon(bitmap)
            .setContentIntent(notificationIntent)
            .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,PlaybackStateCompat.ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.new_episode)
            .addAction(if (isPlaying()) pauseAction else playAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0)
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.
                buildMediaButtonPendingIntent(this,PlaybackStateCompat.ACTION_STOP)))

        return notification.build()
    }

    private fun displayNotification(){
        if(mediaSession.controller.metadata == null){
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val mediaDescription = mediaSession.controller.metadata.description

        GlobalScope.launch {
            val iconUrl = URL(mediaDescription.iconUri.toString())
            val bitmap = BitmapFactory.decodeStream(iconUrl.openStream())

            val notification = createNotification(mediaDescription,bitmap)

            ContextCompat.startForegroundService(
                this@SenfonyMediaService,
                Intent(this@SenfonyMediaService,SenfonyMediaService::class.java))

            startForeground(SenfonyMediaService.NOTIFICATION_ID,notification)
        }

    }


    companion object {
        private const val NOTIFICATION_ID = 1
        private const val PLAYER_CHANNEL_ID = "senfony_player_channel"
        private const val SENFONY_EMPTY_ROOT_MEDIA_ID =
            "senfony_empty_root_media_id"
    }

    override fun onStateChanged() {
        displayNotification()

    }

    override fun onStopPlaying() {
        stopSelf()
        stopForeground(true)
    }

    override fun onPausePlaying() {
        stopForeground(false)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession.controller.transportControls.stop()
    }


}

