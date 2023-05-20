package com.bilalberek.senfony.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bilalberek.senfony.Repository.PodcastRepo
import com.bilalberek.senfony.model.Episode
import com.bilalberek.senfony.model.Podcast
import kotlinx.coroutines.launch
import java.util.Date

class PodcastViewModel(application: Application): AndroidViewModel(application) {

    private val _podcastLiveData = MutableLiveData<PodcastViewData?>()
    val podcastLiveData: LiveData<PodcastViewData?> =  _podcastLiveData

    var podcastRepo : PodcastRepo? = null
    var activePodcastViewData: PodcastViewData? = null

    data class PodcastViewData(
        val subscribed: Boolean = false,
        var feedTitle: String? = "",
        var feedUrl: String? = "",
        var feedDesc:String ? = "",
        var imgUrl: String? = "",
        var episode: List<EpisodeViewData> = listOf()

    )

    data class EpisodeViewData(
        var guid:String? = "",
        var title: String?  = "",
        var description: String? = "",
        var mediaUrl: String = "",
        var releaseDate: Date? = null,
        var duration : String? = ""
    )

    private fun episodesToEpisodeView(episodes: List<Episode>): List<EpisodeViewData>{

        return episodes.map{
            EpisodeViewData(
                it.guid,
                it.title,
                it.description,
                it.mediaUrl,
                it.releaseDate,
                it.duration,

            )
        }
    }

    private fun podcastTOPodcastView(podcast: Podcast): PodcastViewData{
        return PodcastViewData(
            false,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodeView(podcast.episodes)
        )
    }

    suspend fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData){

        podcastSummaryViewData.feedUrl?.let { url ->
            viewModelScope.launch {
                podcastRepo?.getPodcast(url)?.let {
                    it.feedTitle = podcastSummaryViewData.name ?: ""
                    it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                    _podcastLiveData.value = podcastTOPodcastView(it)
                } ?: run {
                    _podcastLiveData.value = null
                }
            }
        } ?: run {
            _podcastLiveData.value = null
        }

    }
}