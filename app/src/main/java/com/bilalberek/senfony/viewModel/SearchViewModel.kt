package com.bilalberek.senfony.viewModel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Build.VERSION.SDK
import android.os.Build.VERSION.SDK_INT
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.bilalberek.senfony.Repository.ItunesRepo
import com.bilalberek.senfony.model.PodcastResponse
import com.bilalberek.senfony.utility.DateUtil
import java.io.IOException

class SearchViewModel(application: Application) :
    AndroidViewModel(application) {

    var iTunesRepo: ItunesRepo? = null

    data class PodcastSummaryViewData(
        var name: String? = "",
        var lastUpdated: String? = "",
        var imageUrl: String? = "",
        var feedUrl: String? = ""
    )


    private fun itunesPodcastToPodcastSummaryView(
        itunesPodcast: PodcastResponse.ItunesPodcast
    ):
            PodcastSummaryViewData {
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
           DateUtil.complexDateToNormalDate(itunesPodcast.releaseDate ?: "-"),
            itunesPodcast.artworkUrl30,
            itunesPodcast.feedUrl
        )
    }

    suspend fun searchPodcasts(term: String):
            List<PodcastSummaryViewData> {

        try {

            if(isInternetAvailable(getApplication())){
                val results = iTunesRepo?.searchByTerm(term)

                if (results != null && results.isSuccessful) {

                    val podcasts = results.body()?.results

                    if (!podcasts.isNullOrEmpty()) {

                        return podcasts.map { podcast ->
                            itunesPodcastToPodcastSummaryView(podcast)
                        }
                    }

                        return emptyList()

                }

            }else{
                Toast.makeText(getApplication(),
                    "checkout internet connection please",
                    Toast.LENGTH_SHORT).show()
            }

        }catch (t:Throwable){
            when(t){
                is IOException ->   Toast.makeText(getApplication(),
                    "checkout internet connection please",
                    Toast.LENGTH_SHORT).show()
            }

        }
        return emptyList()
    }

     fun isInternetAvailable(context : Context): Boolean{

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val  network = connectivityManager.activeNetwork
                ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network)
                ?: return false

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)


        }else{
            val networkInfo = connectivityManager.activeNetworkInfo?: return false
            return networkInfo.isConnected
        }
    }
}