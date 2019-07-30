package com.dgunia.appleappstorereviewlib

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.derby.jdbc.EmbeddedDataSource
import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException
import java.io.IOException
import java.sql.Connection
import java.sql.SQLException
import java.util.*

fun main(args: Array<String>) {
    FindNewReviews(args[0]).findNewReviews { entry ->
        println("${entry.id.label}: ${entry.imRating.label}: ${entry.title.label}")
    }
}

/**
 * Searches for new reviews for the given appid and calls the newReviewListener for each new review it has found.
 */
class FindNewReviews(val appid: String, val database : String = "reviews.derby") {
    fun findNewReviews(newReviewListener: (EntryItem) -> Unit) {
        // Load the reviews
        val reviews = Reviews(appid).loadRecentReviewsForAllLanguages()

        // Save the IDs into a database to check which of them are new.
        val connection = openDatabase()

        reviews.forEach { id, entry ->
            try {
                val stmt = connection!!.prepareStatement("INSERT INTO ReviewIDs(idlabel) VALUES(?)")
                stmt.setLong(1, entry.id.label.toLong())
                stmt.execute()

                // New review found
                newReviewListener(entry)
            } catch(e: DerbySQLIntegrityConstraintViolationException) {
            }
        }
    }

    /**
     * Open the database to save the existing IDs into it.
     */
    private fun openDatabase(): Connection? {
        val dataSource = EmbeddedDataSource()
        dataSource.databaseName = "reviews.derby"
        dataSource.user = "sa"
        dataSource.password = "appstorereviews"
        dataSource.createDatabase = "create"
        dataSource.connectionAttributes = "territory=en"
        val connection = dataSource.connection
        try {
            connection.prepareCall("CREATE TABLE ReviewIDs(idlabel BIGINT, PRIMARY KEY (idlabel))").execute()
        } catch (e: SQLException) {
        }
        return connection
    }
}

/**
 * This class can load recent reviews of an app from iTunes.
 */
class Reviews(val appid: String) {
    companion object {
        val availableLanguages = arrayOf("DZ","AO","AI","AG","AR","AM","AU","AT","AZ","BH","BD","BB","BY","BE","BZ","BM","BO","BW","BR","VG","BN","BG","CA","KY","CL","CN","CO","CR","CI","HR","CY","CZ","DK","DM","DO","EC","EG","SV","EE","FI","FR","DE","GH","GR","GD","GT","GY","HN","HK","HU","IS","IN","ID","IE","IL","IT","JM","JP","JO","KZ","KE","KR","KW","LV","LB","LI","LT","LU","MO","MK","MG","MY","MV","ML","MT","MU","MX","MD","MS","NP","NL","NZ","NI","NE","NG","NO","OM","PK","PA","PY","PE","PH","PL","PT","QA","RO","RU","SA","SN","RS","SG","SK","SI","ZA","ES","LK","KN","LC","VC","SR","SE","CH","TW","TZ","TH","BS","TT","TN","TR","TC","UG","GB","UA","AE","UY","US","UZ","VE","VN","YE")
        val okHttpClient = OkHttpClient()
        val objectMapper = ObjectMapper().apply {
            configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        }
    }

    fun loadRecentReviewsForLanguage(language: String) : ReviewsList {
        val response = okHttpClient.newCall(Request.Builder().url("https://itunes.apple.com/${language.toLowerCase()}/rss/customerreviews/id=$appid/sortBy=mostRecent/json").build()).execute()
        if (response.isSuccessful) {
            return objectMapper.readValue<ReviewsList>(response.body()!!.byteStream(), ReviewsList::class.java)
        }
        throw IOException("Could not download reviews.")
    }

    fun loadRecentReviewsForAllLanguages() : SortedMap<Long, EntryItem> {
        val result = TreeMap<Long, EntryItem>()
        availableLanguages.forEach { lang ->
            loadRecentReviewsForLanguage(lang).feed.entry?.let { entry -> entry.forEach { e -> result.put(e.id.label.toLong(), e) } }
        }
        return result
    }
}