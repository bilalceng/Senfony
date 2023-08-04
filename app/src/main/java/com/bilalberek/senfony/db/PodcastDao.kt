package com.bilalberek.senfony.db

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import com.bilalberek.senfony.model.Episode
import com.bilalberek.senfony.model.Podcast
import kotlinx.coroutines.CoroutineScope
import java.util.*

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

    @Query("SELECT * FROM Podcast WHERE feedUrl = :url ")
    suspend fun loadPodcast(url: String) : Podcast?

    @Delete
    fun deletePodcast(podcast: Podcast)

}

@Database(entities = [Podcast::class,Episode::class], version = 1)
@TypeConverters(Converters::class)

abstract class PodPlayDatabase: RoomDatabase(){

    abstract fun podcastDao(): PodcastDao

    companion object{
        @Volatile
        private var INSTANCE: PodPlayDatabase? = null

        fun getInstance(context: Context, coroutineScope: CoroutineScope): PodPlayDatabase{
            val tempInstance = INSTANCE
            if(tempInstance != null){
                return tempInstance
            }

            synchronized(this){
                Log.d("tarik", Thread.currentThread().name)
                val instance = Room.databaseBuilder(context,PodPlayDatabase::class.java,"PodPlayer")
                    .build()
                INSTANCE = instance

                return INSTANCE as PodPlayDatabase
            }
        }

    }

}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }
    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return (date?.time)
    }
}




