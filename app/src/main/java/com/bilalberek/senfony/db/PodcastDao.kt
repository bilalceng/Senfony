package com.bilalberek.senfony.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import com.bilalberek.senfony.model.Episode
import com.bilalberek.senfony.model.Podcast

@Dao
interface PodcastDao {
    @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
    fun loadPodcasts(): LiveData<List<Podcast>>

    @Query("SELECT * FROM Episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    suspend fun loadEpisodes(podcastId: Long): List<Episode>

    @Insert(onConflict = REPLACE)
    suspend fun insertPodcast(podcast: Podcast): Long

    @Insert(onConflict = REPLACE)
    suspend fun insertEpisode(episode: Episode): Long

}

@Database(entities = [Podcast::class,Episode::class], version = 1)

abstract class PodPlayDatabase(): RoomDatabase(){

    abstract fun podcastDao(): PodcastDao


}
