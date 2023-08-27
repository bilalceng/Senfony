package com.bilalberek.senfony.viewModel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.bilalberek.senfony.Repository.ItunesRepo
import com.bilalberek.senfony.model.PodcastResponse
import com.bilalberek.senfony.utility.DateUtil
import java.net.UnknownHostException

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
    ): PodcastSummaryViewData {
        return PodcastSummaryViewData(
            itunesPodcast.collectionCensoredName,
           DateUtil.complexDateToNormalDate(itunesPodcast.releaseDate ?: "-"),
            itunesPodcast.artworkUrl100,
            itunesPodcast.feedUrl
        )
    }

    suspend fun searchPodcasts(term: String):
            List<PodcastSummaryViewData> {
        Log.d("yarak" ,"  termn3 :${term}")

        try {
            Log.d("yarak" ,"  termn4 :${term}")

            try {
                val results = iTunesRepo?.searchByTerm(term)
            }catch (e: UnknownHostException){
                Toast.makeText(getApplication(), "" +
                        "http error occured", Toast.LENGTH_SHORT).show()
            }

                val results = iTunesRepo?.searchByTerm(term)
            Log.d("yarak" ,"meraba")

                if (results != null && results.isSuccessful) {

                    val podcasts = results.body()?.results

                    if (!podcasts.isNullOrEmpty()) {

                        return podcasts.map { podcast ->
                            itunesPodcastToPodcastSummaryView(podcast)
                        }
                    }

                        return emptyList()

                }



        }catch (e:Exception){

                 Toast.makeText(getApplication(),
                    "checkout internet connection please",
                    Toast.LENGTH_SHORT).show()
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