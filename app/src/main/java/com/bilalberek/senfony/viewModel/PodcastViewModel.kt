package com.bilalberek.senfony.viewModel

import android.app.Application
import android.text.format.DateUtils
import android.widget.Toast
import androidx.lifecycle.*
import com.bilalberek.senfony.Repository.PodcastRepo
import com.bilalberek.senfony.db.PodPlayDatabase
import com.bilalberek.senfony.db.PodcastDao
import com.bilalberek.senfony.model.Episode
import com.bilalberek.senfony.model.Podcast
import com.bilalberek.senfony.utility.DateUtil.dateToShortDate
import kotlinx.coroutines.launch
import java.util.Date


class PodcastViewModel(application: Application): AndroidViewModel(application) {

    var livePodcastSummaryViewData:LiveData<List<SearchViewModel.PodcastSummaryViewData>>?  = null
    //!!!!!!!!!!!!
    private val _podcastLiveData = MutableLiveData<PodcastViewData?>()
    val podcastLiveData: LiveData<PodcastViewData?> =  _podcastLiveData
    //!!!!!!!!!!!'!
    var podcastRepo : PodcastRepo? = null
    var activeEpisodeViewData: EpisodeViewData? = null
    val podcastDao: PodcastDao = PodPlayDatabase.getInstance(application,viewModelScope).podcastDao()
    var activePodcast: Podcast? = null

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
            podcast.id != null,
            podcast.feedTitle,
            podcast.feedUrl,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodeView(podcast.episodes)
        )
    }

    private fun podcastToSummaryView(podcast: Podcast): SearchViewModel.PodcastSummaryViewData{
        return SearchViewModel.PodcastSummaryViewData(
            podcast.feedTitle,
            dateToShortDate(podcast.lastUpdated),
            podcast.imageUrl,
            podcast.feedUrl
        )
    }



     fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData){

         podcastSummaryViewData.feedUrl?.let { url ->
            viewModelScope.launch {
                podcastRepo?.getPodcast(url)?.let {
                    it.feedTitle = podcastSummaryViewData.name ?: ""
                    it.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                    _podcastLiveData.postValue(podcastTOPodcastView(it))
                    activePodcast = it
                } ?: run {
                    _podcastLiveData.postValue(null)
                }
            }
        } ?: run {
            _podcastLiveData.value = null
        }

    }

    fun saveActivePodcast(){
        activePodcast?.let {
            it.episodes = it.episodes.drop(1)
            podcastRepo?.save(it) ?: return
        }
    }

    fun getPodcasts(): LiveData<List<SearchViewModel.PodcastSummaryViewData>>?{
        if (livePodcastSummaryViewData == null){
            livePodcastSummaryViewData = podcastRepo?.getAll()?.map { podcastList ->
                podcastList.map { podcast ->
                    podcastToSummaryView(podcast)

                }
            }
        }
        return livePodcastSummaryViewData
    }

    fun deleteActivePodcast(){
        activePodcast?.let { podcast ->
            podcastRepo?.delete(podcast)
        }

    }

    suspend fun setActivePodcast(feedUrl: String):
            SearchViewModel.PodcastSummaryViewData?{
        val repo =  podcastRepo ?: return null
        val podcast = repo.getPodcast(feedUrl) ?: return null
        _podcastLiveData.value = podcastTOPodcastView(podcast)
        activePodcast = podcast

        return podcastToSummaryView(podcast)
    }
}




