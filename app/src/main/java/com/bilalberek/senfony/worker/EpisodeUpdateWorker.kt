package com.bilalberek.senfony.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bilalberek.senfony.R
import com.bilalberek.senfony.Repository.PodcastRepo
import com.bilalberek.senfony.db.PodPlayDatabase
import com.bilalberek.senfony.service.RssFeedService
import com.bilalberek.senfony.ui.PodcastActivity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


class EpisodeUpdateWorker(context: Context, params: WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork(): Result  = coroutineScope {

        val job = launch {
            val db = PodPlayDatabase.getInstance(applicationContext,this)
            val repo = PodcastRepo(RssFeedService.instance,db.podcastDao())

            val podcastUpdates = repo.updatePodcastEpisodes()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            }

            for (podcastUpdate in podcastUpdates){
                displayNotification(podcastUpdate)
            }
        }
        job.join()
        Result.success()
    }

    companion object{
      const  val EPISODE_CHANNEL_ID = "senfony_episode_channel"
      const val EXTRA_FEED_URL = "podcast_feed_url"
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(){
        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE)
        as NotificationManager

        if(notificationManager.getNotificationChannel(EPISODE_CHANNEL_ID) == null){
            val channel = NotificationChannel(EPISODE_CHANNEL_ID,"Episodes", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
}

    private fun displayNotification(podcastInfo: PodcastRepo.PodcastUpdateInfo){
        val contentIntent = Intent(applicationContext,PodcastActivity::class.java)
        contentIntent.putExtra(EXTRA_FEED_URL,podcastInfo.feedUrl)

        val pendingContent = PendingIntent.getActivity(applicationContext,
            0,contentIntent,PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(applicationContext, EPISODE_CHANNEL_ID)
            .setSmallIcon(R.drawable.new_episode)
            .setContentTitle(applicationContext.getString(R.string.episode_notification_title))
            .setContentText(applicationContext.getString(R.string.episode_notification_text))
            .setNumber(podcastInfo.newCount)
            .setAutoCancel(true)
            .setContentIntent(pendingContent)
            .build()

        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(podcastInfo.name,0,notification)

    }
}