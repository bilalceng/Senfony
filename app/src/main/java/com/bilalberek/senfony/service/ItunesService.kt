package com.bilalberek.senfony.service

import com.bilalberek.senfony.model.PodcastResponse
import com.bilalberek.senfony.utility.Utility
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesService {

    @GET("/search?media=podcast")

    suspend fun searchMusicByTerm(@Query("term") term: String):
            Response<PodcastResponse>

    companion object{

        private val retrofit by lazy{
            val logging  = HttpLoggingInterceptor()
            logging .setLevel(HttpLoggingInterceptor.Level.BODY)

            val client  = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            Retrofit.Builder()
                .baseUrl(Utility.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }

        val instance by lazy {
            retrofit.create(ItunesService::class.java)
        }
    }
}