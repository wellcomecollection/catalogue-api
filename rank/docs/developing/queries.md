# Developing queries

Our queries are the easiest part of the search-relevance puzzle to modify and test.

- Make your changes to [`WorksMultiMatcherQuery.json`](/search/src/test/resources/WorksMultiMatcherQuery.json) or [`ImagesMultiMatcherQuery.json`](/search/src/test/resources/ImagesMultiMatcherQuery.json).
- Use the `candidate` queryEnv on `/dev` or `/search` to see the results.
- When you're happy with the effect of your changes on the rank tests, you'll need to make the scala used by the API match the JSON used by rank. Edit the [images](images-scala-file) and/or [works](works-scala-file) scala files until [the tests](scala-tests) pass.

[works-scala-file]: /search/src/main/scala/weco/api/search/elasticsearch/WorksMultiMatcher.scala
[images-scala-file]: /search/src/test/scala/weco/api/search/images/ImagesSimilarityTest.scala
[scala-tests]: /search/src/test/scala/weco/api/search/elasticsearch/SearchQueryJsonTest.scala
