# AppStoreReviewLib
A Kotlin library to find new reviews in the Apple AppStore

E.g. to find all new reviews for app "123456" just use it like this:

```Kotlin
FindNewReviews("123456").getNewReviews().forEach { entry ->
    println("${entry.id.label}: ${entry.imRating.label}: ${entry.title.label}")
}
```
This will print all new reviews that have been written since the last time the program was run. For this purpose it saves
all known review IDs into an Apache Derby database on each run.

## Command Line Usage

You can use it from the command line to send you emails for all new reviews. First compile the project:
```
mvn clean compile assembly:single
```

This will create a file target/AppStoreReviewsLib-1.0-jar-with-dependencies.jar that you can start:

```
java -jar target/AppStoreReviewsLib-1.0-jar-with-dependencies.jar -a 123456
```

This will read all reviews, print them to the console and write their IDs into a database. Afterward it will
know that it has already seen these reviews. Now you can run the command

```
java -jar target/AppStoreReviewsLib-1.0-jar-with-dependencies.jar -a 123456 -e -c email.ini
```

from time to time to check for new reviews and automatically send emails. The file email.ini contains the
configuration for your email server and should look like this:

```
host=mail.yourserver.com
port=465
user=yourusername
password=yourpassword
receiver=receiver@yourserver.com
from=sender@yourserver.com
```

