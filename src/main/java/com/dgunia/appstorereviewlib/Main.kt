package com.dgunia.appstorereviewlib

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.cli.*
import org.apache.derby.jdbc.EmbeddedDataSource
import org.apache.derby.shared.common.error.DerbySQLIntegrityConstraintViolationException
import java.io.FileInputStream
import java.io.IOException
import java.sql.Connection
import java.sql.SQLException
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

val ARG_APPID = "appid"
val ARG_APPNAME = "appname"
val ARG_CONFIG = "config"

fun main(args: Array<String>) {
    val options = Options()
    options.addOption(Option.builder("a").longOpt("appid").desc("The id of the app.").hasArg().argName(ARG_APPID).required().build())
    options.addOption(Option.builder("n").longOpt("appname").desc("The name of the app for the email subject.").hasArg().argName(ARG_APPNAME).build())
    options.addOption(Option.builder("h").longOpt("help").desc("Help").build())
    options.addOption(Option.builder("e").longOpt("sendemails").desc("Send an email for each new review.").build())
    options.addOption(Option.builder("c").longOpt("config").hasArg().argName(ARG_CONFIG).desc("Config file for the email configuration.").build())

    try {
        val parser = DefaultParser()
        val cmd = parser.parse(options, args)

        if (cmd.hasOption("h")) {
            showHelp(options)
            return
        }

        if (!cmd.hasOption(ARG_APPID)) {
            println("You have to specify an appid with \"-a 123456\".")
            return
        }

        if (cmd.hasOption("e") && !cmd.hasOption("c")) {
            println("You have to specify a config file with your email server login.")
            return
        }

        val appid = cmd.getOptionValue(ARG_APPID)
        val sendEmails = cmd.hasOption("e")

        // Load new reviews
        val newReviews = FindNewReviews(appid).getNewReviews()

        val prop by lazy { Properties().apply { load(FileInputStream(cmd.getOptionValue(ARG_CONFIG))) } }

        // Print and email new reviews
        newReviews.forEach { entry ->
            println("${entry.id.label}: ${entry.imRating.label}: ${entry.title.label}")

            if (sendEmails) {
                sendEmail(entry, appid, cmd.getOptionValue(ARG_APPNAME) ?: appid, prop.getProperty("host"), prop.getProperty("port").toInt(), prop.getProperty("user"), prop.getProperty("password"), prop.getProperty("receiver"), prop.getProperty("from"))
            }
        }
    } catch (e: MissingOptionException) {
        showHelp(options)
    }
}

fun sendEmail(entry: EntryItem, appid: String, appname: String, smtpHost : String, smtpPort : Int, smtpUsername : String, smtpPassword : String, mailReceiver: String, mailFrom: String) {
    val props = Properties()
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.smtp.host", smtpHost)
    props.put("mail.smtp.port", smtpPort)

    val session = Session.getInstance(props, null)
    val transport = session.getTransport("smtps")
    transport!!.connect(smtpHost, smtpPort, smtpUsername, smtpPassword)

    val message = MimeMessage(session)
    message.setFrom(InternetAddress(mailFrom))
    message.subject = "New review for $appname: (${entry.imRating.label}): ${entry.title.label}"
    message.setRecipients(Message.RecipientType.TO, mailReceiver)
    message.setContent(getHTMLEmailBodyForEntry(entry, appid), "text/html; charset=UTF-8")
    transport.sendMessage(message, InternetAddress.parse(mailReceiver))
}

fun getHTMLEmailBodyForEntry(entry: EntryItem, appid: String): String {
    return """<html><head></head><body><p>Rating: ${"⭐⭐⭐⭐⭐".substring(0, entry.imRating.label.toInt())}</p><p>${entry.author.name.label}: <b>${entry.title.label}</b></p><p>${entry.content.label}<p><p><a href="https://appstoreconnect.apple.com/WebObjects/iTunesConnect.woa/ra/ng/app/$appid/activity/ios/ratingsResponses">Open App Store Connect Reviews Page</a></p></body></html>"""
}

private fun showHelp(options: Options) {
    HelpFormatter().printHelp("java appstorereviewlib", options);
}

/**
 * Searches for new reviews for the given appid and calls the newReviewListener for each new review it has found.
 */
class FindNewReviews(val appid: String, val database: String = "reviews$appid.derby") {
    /**
     * Returns a list of reviews that were added since the last time the program was run.
     */
    fun getNewReviews(): List<EntryItem> {
        val result = ArrayList<EntryItem>()
        findNewReviews { result.add(it) }
        return result
    }

    private fun findNewReviews(newReviewListener: (EntryItem) -> Unit) {
        val connection = createDatabaseConnection() ?: throw IOException("Could not open database $database")

        // Load the reviews
        val reviews = Reviews(appid).loadRecentReviewsForAllLanguages()

        // Save the IDs into a database to check which of them are new.
        reviews.forEach { id, entry ->
            try {
                val stmt = connection.prepareStatement("INSERT INTO ReviewIDs(idlabel) VALUES(?)")
                stmt.setLong(1, entry.id.label.toLong())
                stmt.execute()

                // New review found
                newReviewListener(entry)
            } catch (e: DerbySQLIntegrityConstraintViolationException) {
            }
        }

        connection.close()
    }

    /**
     * Opens the database to save the existing IDs into it.
     */
    private fun createDatabaseConnection(): Connection? {
        val dataSource = EmbeddedDataSource()
        dataSource.databaseName = database
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
        val availableLanguages = arrayOf("DZ", "AO", "AI", "AG", "AR", "AM", "AU", "AT", "AZ", "BH", "BD", "BB", "BY", "BE", "BZ", "BM", "BO", "BW", "BR", "VG", "BN", "BG", "CA", "KY", "CL", "CN", "CO", "CR", "CI", "HR", "CY", "CZ", "DK", "DM", "DO", "EC", "EG", "SV", "EE", "FI", "FR", "DE", "GH", "GR", "GD", "GT", "GY", "HN", "HK", "HU", "IS", "IN", "ID", "IE", "IL", "IT", "JM", "JP", "JO", "KZ", "KE", "KR", "KW", "LV", "LB", "LI", "LT", "LU", "MO", "MK", "MG", "MY", "MV", "ML", "MT", "MU", "MX", "MD", "MS", "NP", "NL", "NZ", "NI", "NE", "NG", "NO", "OM", "PK", "PA", "PY", "PE", "PH", "PL", "PT", "QA", "RO", "RU", "SA", "SN", "RS", "SG", "SK", "SI", "ZA", "ES", "LK", "KN", "LC", "VC", "SR", "SE", "CH", "TW", "TZ", "TH", "BS", "TT", "TN", "TR", "TC", "UG", "GB", "UA", "AE", "UY", "US", "UZ", "VE", "VN", "YE")
        val okHttpClient = OkHttpClient()
        val objectMapper = ObjectMapper().apply {
            configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        }
    }

    fun loadRecentReviewsForLanguage(language: String): ReviewsList {
        val response = okHttpClient.newCall(Request.Builder().url("https://itunes.apple.com/${language.toLowerCase()}/rss/customerreviews/id=$appid/sortBy=mostRecent/json").build()).execute()
        if (response.isSuccessful) {
            return objectMapper.readValue<ReviewsList>(response.body()!!.byteStream(), ReviewsList::class.java)
        }
        throw IOException("Could not download reviews.")
    }

    fun loadRecentReviewsForAllLanguages(): SortedMap<Long, EntryItem> {
        val result = TreeMap<Long, EntryItem>()
        availableLanguages.forEach { lang ->
            loadRecentReviewsForLanguage(lang).feed.entry?.let { entry -> entry.forEach { e -> result.put(e.id.label.toLong(), e) } }
        }
        return result
    }
}