package com.dgunia.appstorereviewlib


import com.fasterxml.jackson.annotation.JsonProperty

data class Updated(@JsonProperty("label")
                   val label: String = "")


data class ImVersion(@JsonProperty("label")
                     val label: String = "")


data class Attributes(@JsonProperty("rel")
                      val rel: String? = "",
                      @JsonProperty("href")
                      val href: String? = "",
                      @JsonProperty("term")
                      val term: String? = "",
                      @JsonProperty("label")
                      val label: String? = "",
                      @JsonProperty("type")
                      val type: String? = "")


data class Title(@JsonProperty("label")
                 val label: String = "")


data class ImVoteSum(@JsonProperty("label")
                     val label: String = "")


data class ReviewsList(@JsonProperty("feed")
                  val feed: Feed)


data class ImVoteCount(@JsonProperty("label")
                       val label: String = "")


data class Uri(@JsonProperty("label")
               val label: String = "")


data class Name(@JsonProperty("label")
                val label: String = "")


data class Rights(@JsonProperty("label")
                  val label: String = "")


data class ImRating(@JsonProperty("label")
                    val label: String = "")


data class Content(@JsonProperty("attributes")
                   val attributes: Attributes,
                   @JsonProperty("label")
                   val label: String = "")


data class ImContentType(@JsonProperty("attributes")
                         val attributes: Attributes)


data class Author(@JsonProperty("name")
                  val name: Name,
                  @JsonProperty("label")
                  val label: String? = "",
                  @JsonProperty("uri")
                  val uri: Uri)


data class Id(@JsonProperty("label")
              val label: String = "")


data class Icon(@JsonProperty("label")
                val label: String = "")


data class Link(@JsonProperty("attributes")
                val attributes: Attributes)


data class EntryItem(@JsonProperty("im:voteCount")
                     val imVoteCount: ImVoteCount,
                     @JsonProperty("im:contentType")
                     val imContentType: ImContentType,
                     @JsonProperty("im:rating")
                     val imRating: ImRating,
                     @JsonProperty("author")
                     val author: Author,
                     @JsonProperty("link")
                     val link: Link,
                     @JsonProperty("im:version")
                     val imVersion: ImVersion,
                     @JsonProperty("im:voteSum")
                     val imVoteSum: ImVoteSum,
                     @JsonProperty("id")
                     val id: Id,
                     @JsonProperty("title")
                     val title: Title,
                     @JsonProperty("content")
                     val content: Content)


data class Feed(@JsonProperty("entry")
                val entry: List<EntryItem>?,
                @JsonProperty("author")
                val author: Author,
                @JsonProperty("rights")
                val rights: Rights,
                @JsonProperty("icon")
                val icon: Icon,
                @JsonProperty("link")
                val link: List<Link>?,
                @JsonProperty("id")
                val id: Id,
                @JsonProperty("title")
                val title: Title,
                @JsonProperty("updated")
                val updated: Updated)


