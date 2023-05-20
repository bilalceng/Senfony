package com.bilalberek.senfony.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date


@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Podcast::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("podcastId")]
)
data class Episode(
    @PrimaryKey
    var guid : String = "",
    val podcastId: Long? = null,
    var title: String = "",
    var description:String = "",
    var mediaUrl: String = "",
    var mimType : String = "",
    var releaseDate:Date = Date(),
    var duration: String = ""

)
@Entity()
data class Podcast(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    var feedUrl: String = "",
    var feedTitle: String = "",
    var feedDesc: String = "",
    var imageUrl: String = "",
    var lastUpdated: Date = Date(),
    var episodes: List<Episode> = listOf()
)
