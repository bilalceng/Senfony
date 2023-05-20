package com.bilalberek.senfony.utility

import android.os.Build


import android.text.Html
import android.text.Spanned
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
class Utility {
    companion object{
        const val  BASE_URL ="https://itunes.apple.com"
    }
}

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null

) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String,data: T? = null): Resource<T>(data , message)
    class Loading<T>: Resource<T>()
}

object DateUtil{

    fun complexDateToNormalDate(complexDate: String): String{

        val normalFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
        Locale.getDefault())

        val date = normalFormat.parse(complexDate) ?: "-"

        val outputFormat = DateFormat.getDateInstance(DateFormat.SHORT,
        Locale.getDefault())

        return outputFormat.format(date)
    }

    fun xmlDateToNormalDate(dateString: String?): Date {
        val date = dateString ?: return Date()
        val inFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
        Locale.getDefault())

        return inFormat.parse(date) ?: Date()
    }

    fun dateToShortDate(date: Date): String{
        val outputFormat = DateFormat.getDateInstance(
            DateFormat.SHORT, Locale.getDefault())
        return outputFormat.format(date)
    }

}



    object HtmlUtils {
        fun htmlToSpannable(htmlDesc: String): Spanned {

            var newHtmlDesc = htmlDesc.replace("\n".toRegex(), "")

            newHtmlDesc = newHtmlDesc.replace("(<(/)img>)|(<img.+?>)".toRegex(), "")

            val document: Document = Jsoup.parse(newHtmlDesc)

            val cleanedHtml: String = Jsoup.clean(
                document.body().html(),
                Whitelist.basic()
            )
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(cleanedHtml, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(cleanedHtml)
            }
        }
    }

