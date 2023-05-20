package com.bilalberek.senfony.Repository

import com.bilalberek.senfony.model.Episode
import com.bilalberek.senfony.model.Podcast
import com.bilalberek.senfony.service.FeedService
import com.bilalberek.senfony.service.RssFeedResponse
import com.bilalberek.senfony.service.RssFeedService
import com.bilalberek.senfony.utility.DateUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo (private var feedService: RssFeedService){



     suspend fun getPodcast(feedUrl : String): Podcast?{
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
                    DateUtil.xmlDateToNormalDate(it.pubDate),
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
}