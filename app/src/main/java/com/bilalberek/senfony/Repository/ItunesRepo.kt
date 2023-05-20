package com.bilalberek.senfony.Repository

import com.bilalberek.senfony.service.ItunesService


class ItunesRepo(private val itunesService: ItunesService) {

    suspend fun searchByTerm(term: String) =
        itunesService.searchMusicByTerm(term)



}

