package com.bilalberek.senfony.Repository


import android.util.Log
import androidx.lifecycle.LiveData
import com.bilalberek.senfony.db.PodcastDao
import com.bilalberek.senfony.model.Episode
import com.bilalberek.senfony.model.Podcast
import com.bilalberek.senfony.service.FeedService
import com.bilalberek.senfony.service.RssFeedResponse
import com.bilalberek.senfony.service.RssFeedService
import com.bilalberek.senfony.utility.DateUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class PodcastRepo (private var feedService: RssFeedService,
                     var podcastDao: PodcastDao
){
suspend fun getPodcast(feedUrl : String): Podcast?{

        val podcastLocal = podcastDao.loadPodcast(feedUrl)
        if (podcastLocal != null) {
            podcastLocal.id?.let {
                podcastLocal.episodes = podcastDao.loadEpisodes(it)
                return podcastLocal
            }
        }

             var podcast: Podcast? = null
             val feedResponse = feedService.getFeed(feedUrl)
             if (feedResponse != null) {
                 podcast = rssResponseToPodcast(feedUrl, "", feedResponse)

             }
             return podcast

         }





    private fun rssItemsToEpisodes(episodeResponses: List<RssFeedResponse.EpisodeResponse>): List<Episode>{
            return episodeResponses.map{
                Episode(
                    it.guid ?: "",
                    null,
                    it.title ?: "",
                    it.description ?: "",
                    it.url ?: "",
                    it.type ?: "",
                    Date(it.pubDate),
                    it.duration ?: ""
                )
            }
    }

    private fun rssResponseToPodcast(feedUrl: String, imageUrl: String, rssFeedResponse: RssFeedResponse): Podcast?{
        val items = rssFeedResponse.episodes ?: return null

        val description = if (rssFeedResponse.description == ""){
            rssFeedResponse.summary
        }else{
            rssFeedResponse.description
        }

        return Podcast(null,feedUrl,rssFeedResponse.title,description,imageUrl,rssFeedResponse.lastUpdated,rssItemsToEpisodes(items))
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun save(podcast: Podcast){
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)

            for(episode in podcast.episodes){
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)

            }
        }
    }

    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }

    fun delete(podcast: Podcast){
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }


    private suspend fun  getNewEpisodes(localPodcast: Podcast): List<Episode>{
        val response = feedService.getFeed(localPodcast.feedUrl)
        if(response != null){
            val remotePodcast = rssResponseToPodcast(localPodcast.feedUrl,
            localPodcast.imageUrl,
            response)

            remotePodcast?.let {
                val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)
                return remotePodcast.episodes.filter { episode ->
                    localEpisodes.find { episode.guid == it.guid } == null
                }

                }
            }
        return listOf()
        }


    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode> ){

        GlobalScope.launch {
            for( episode in episodes){
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    suspend fun updatePodcastEpisodes(): MutableList<PodcastUpdateInfo>{
        val updatedPodcast = mutableListOf<PodcastUpdateInfo>()
        val podcasts = podcastDao.loadSubscribedPodcastList()

        for (podcast in podcasts){
            val newEpisodes = getNewEpisodes(podcast)
            if (newEpisodes.isNotEmpty()){
                podcast.id?.let {
                    saveNewEpisodes(it, newEpisodes)
                    updatedPodcast.add(PodcastUpdateInfo(podcast.feedUrl,podcast.feedTitle,
                        newEpisodes.count()))
                }
            }
        }
        return updatedPodcast
    }

    inner class PodcastUpdateInfo(
        val feedUrl: String,
        val name: String,
        val newCount: Int
    )
    }
