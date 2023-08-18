package com.bilalberek.senfony.Repository

import android.util.Log
import com.bilalberek.senfony.model.PodcastResponse
import com.bilalberek.senfony.service.ItunesService
import retrofit2.Response


class ItunesRepo(private val itunesService: ItunesService) {

    suspend fun searchByTerm(term: String): Response<PodcastResponse> {
        Log.d("yarak" ,"  termn4 :${term}")
       return itunesService.searchMusicByTerm(term)
    }


}

