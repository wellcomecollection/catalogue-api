package weco.api.search.models

import com.sksamuel.elastic4s.requests.common.Shards
import com.sksamuel.elastic4s.requests.searches.{
  SearchHits,
  SearchResponse,
  Total
}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AggregationResultsTest extends AnyFunSpec with Matchers {
  it("destructures a single aggregation result") {
    val searchResponse = SearchResponse(
      took = 1234,
      isTimedOut = false,
      isTerminatedEarly = false,
      suggest = Map(),
      _shards = Shards(total = 1, failed = 0, successful = 1),
      scrollId = None,
      hits = SearchHits(
        total = Total(0, "potatoes"),
        maxScore = 0.0,
        hits = Array()
      ),
      _aggregationsAsMap = Map(
        "format" -> Map(
          "format" -> Map(
            "doc_count_error_upper_bound" -> 0,
            "sum_other_doc_count" -> 0,
            "buckets" -> List(
              Map(
                "key" -> "apple",
                "doc_count" -> 393145
              ),
              Map(
                "key" -> "banana",
                "doc_count" -> 5696
              ),
              Map(
                "key" -> "coconut",
                "doc_count" -> 9
              )
            )
          )
        )
      )
    )
    println(searchResponse)
    val singleAgg = WorkAggregations(searchResponse)
    singleAgg.get.format shouldBe Some(
      Aggregation(
        buckets = List(
          AggregationBucket(
            AggregationBucketData("apple", "apple"),
            count = 393145
          ),
          AggregationBucket(
            AggregationBucketData("banana", "banana"),
            count = 5696
          ),
          AggregationBucket(
            AggregationBucketData("coconut", "coconut"),
            count = 9
          )
        )
      )
    )
  }

  it("populates AggregationBucketData with the same label and ID if no nested 'labels' bucket provided") {
    val searchResponse = SearchResponse(
      took = 1234,
      isTimedOut = false,
      isTerminatedEarly = false,
      suggest = Map(),
      _shards = Shards(total = 1, failed = 0, successful = 1),
      scrollId = None,
      hits = SearchHits(
        total = Total(0, "potatoes"),
        maxScore = 0.0,
        hits = Array()
      ),
      _aggregationsAsMap = Map(
        "format" -> Map(
          "format" -> Map(
            "doc_count_error_upper_bound" -> 0,
            "sum_other_doc_count" -> 0,
            "buckets" -> List(
              Map(
                "key" -> "artichoke",
                "doc_count" -> 393145
              )
            )
          )
        )
      )
    )
    val singleAgg = WorkAggregations(searchResponse)
    singleAgg.get.format shouldBe Some(
      Aggregation(
        buckets = List(
          AggregationBucket(
            AggregationBucketData("artichoke", "artichoke"),
            count = 393145
          )
        )
      )
    )
  }

  it("correctly populates AggregationBucketData with IDs and labels if a nested 'labels' bucket is provided for each ID") {
    val searchResponse = SearchResponse(
      took = 1234,
      isTimedOut = false,
      isTerminatedEarly = false,
      suggest = Map(),
      _shards = Shards(total = 1, failed = 0, successful = 1),
      scrollId = None,
      hits = SearchHits(
        total = Total(0, "potatoes"),
        maxScore = 0.0,
        hits = Array()
      ),
      _aggregationsAsMap = Map(
        "format" -> Map(
          "doc_count" -> 12345,
          "format" -> Map(
            "doc_count_error_upper_bound" -> 0,
            "sum_other_doc_count" -> 0,
            "buckets" -> List(
              Map(
                "key" -> "123",
                "doc_count" -> 393145,
                "labels" -> Map(
                  "buckets" -> List(
                    Map(
                      "key" -> "absinthe",
                      "doc_count" -> 393145
                    )
                  )
                )
              ),
              Map(
                "key" -> "456",
                "doc_count" -> 34,
                "labels" -> Map(
                  "buckets" -> List(
                    Map(
                      "key" -> "apple",
                      "doc_count" -> 34
                    )
                  )
                )
              )
            ),
          )
        )
      )
    )
    val singleAgg = WorkAggregations(searchResponse)
    singleAgg.get.format shouldBe Some(
      Aggregation(
        buckets = List(
          AggregationBucket(
            AggregationBucketData("123", "absinthe"),
            count = 393145
          )
        )
      )
    )
  }
}
