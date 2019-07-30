# AppStoreReviewLib
A Kotlin library to find new reviews in the Apple AppStore

E.g. to find all new reviews for app "123456" just use it like this:

```Kotlin
FindNewReviews("123456").findNewReviews { entry ->
    println("${entry.id.label}: ${entry.imRating.label}: ${entry.title.label}")
}
```
This will print all new reviews that have been written since the last time the program was run. For this purpose it saves
all known review IDs into an Apache Derby database on each run.