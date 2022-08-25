# Query structure

## Complete query json

<details>
<summary>Click to expand</summary>

```json
{
  "bool": {
    "should": [
      {
        "span_first": {
          "match": {
            "span_term": {
              "data.title.shingles": "{{query}}"
            }
          },
          "end": 1,
          "boost": 1000
        }
      },
      {
        "multi_match": {
          "query": "{{query}}",
          "fields": [
            "state.canonicalId^1000.0",
            "state.sourceIdentifier.value^1000.0",
            "data.otherIdentifiers.value^1000.0",
            "data.items.id.canonicalId^1000.0",
            "data.items.id.sourceIdentifier.value^1000.0",
            "data.items.id.otherIdentifiers.value^1000.0",
            "data.imageData.id.canonicalId^1000.0",
            "data.imageData.id.sourceIdentifier.value^1000.0",
            "data.imageData.id.otherIdentifiers.value^1000.0",
            "data.referenceNumber^1000.0"
          ],
          "type": "best_fields",
          "analyzer": "whitespace_analyzer",
          "operator": "Or",
          "_name": "identifiers"
        }
      },
      {
        "dis_max": {
          "queries": [
            {
              "multi_match": {
                "query": "{{query}}",
                "fields": [
                  "search.titlesAndContributors^100.0",
                  "search.titlesAndContributors.english^100.0",
                  "search.titlesAndContributors.shingles^100.0"
                ],
                "type": "best_fields",
                "operator": "And",
                "_name": "title and contributor exact spellings"
              }
            },
            {
              "multi_match": {
                "query": "{{query}}",
                "fields": [
                  "search.titlesAndContributors.arabic",
                  "search.titlesAndContributors.bengali",
                  "search.titlesAndContributors.french",
                  "search.titlesAndContributors.german",
                  "search.titlesAndContributors.hindi",
                  "search.titlesAndContributors.italian"
                ],
                "type": "best_fields",
                "operator": "And",
                "_name": "non-english titles and contributors"
              }
            }
          ]
        }
      },

      {
        "multi_match": {
          "query": "{{query}}",
          "fields": [
            "data.contributors.agent.label^1000.0",
            "data.subjects.concepts.label^10.0",
            "data.genres.concepts.label^10.0",
            "data.production.*.label^10.0",
            "data.description",
            "data.physicalDescription",
            "data.language.label",
            "data.edition",
            "data.notes.contents",
            "data.lettering"
          ],
          "type": "cross_fields",
          "operator": "And",
          "_name": "data"
        }
      }
    ],
    "filter": [
      {
        "term": {
          "type": {
            "value": "Visible"
          }
        }
      }
    ],
    "minimum_should_match": "1"
  }
}
```

</details>

## Intentions

The following section lists the broad intentions which we try to reflect in the structure of our query. [Rank](../../../rank/) uses a list of search term / work ID pairs which illustrate these intentions to validate the performance of each candidate query. The precise examples for works can be seen [here](../../../rank/data/tests/works.ts).

### Users should be able to match against general work data

We include a `multi_match` on a list of the work's core fields

```json
{
  "multi_match": {
    "query": "{{query}}",
    "fields": [
      "data.contributors.agent.label^1000.0",
      "data.subjects.concepts.label^10.0",
      "data.genres.concepts.label^10.0",
      "data.production.*.label^10.0",
      "data.description",
      "data.physicalDescription",
      "data.language.label",
      "data.edition",
      "data.notes.contents",
      "data.lettering"
    ],
    "type": "cross_fields",
    "operator": "And",
    "_name": "data"
  }
}
```

### Users should be able to match documents by their identifiers

Identifiers are `multi_match`ed and heavily boosted so that matches will always appear at the top of the list.

```json
{
  "multi_match": {
    "query": "{{query}}",
    "fields": [
      "state.canonicalId^1000.0",
      "state.sourceIdentifier.value^1000.0",
      "data.otherIdentifiers.value^1000.0",
      "data.items.id.canonicalId^1000.0",
      "data.items.id.sourceIdentifier.value^1000.0",
      "data.items.id.otherIdentifiers.value^1000.0",
      "data.imageData.id.canonicalId^1000.0",
      "data.imageData.id.sourceIdentifier.value^1000.0",
      "data.imageData.id.otherIdentifiers.value^1000.0",
      "data.referenceNumber^1000.0"
    ],
    "type": "best_fields",
    "analyzer": "whitespace_analyzer",
    "operator": "Or",
    "_name": "identifiers"
  }
}
```

### Users should be able to match archive identifiers

We include a `match` on the `search.relations` field.

```json
{
  "match": {
    "search.relations": {
      "query": "{{query}}",
      "_name": "relations",
      "boost": 1000,
      "operator": "AND"
    }
  }
}
```

The field is analysed with a `with_slashes_char_filter`, which allows us to capture hierarchical IDs like `PP/CRI/A` as a single token. Slashes are converted to `__`s and the query is split on `whitespace`. See <https://github.com/wellcomecollection/catalogue-pipeline/pull/1654>

```json
{
  "with_slashes_char_filter" : {
    "type" : "mapping",
    "mappings" : [
      "/=> __"
    ]
  }
}
```

The field is heavily boosted to ensure that matching documents appear at the top of the list of results.

### Users should be able to match documents by their title and contributors (in the same query)

For example, a user might want to find [_Time lapse_ by Cassils](https://wellcomecollection.org/works/ftqy78zj) by searching for "cassils time lapse".

We construct a `search.titlesAndContributors` field with data copied from the title and contributors fields, and analyse it in multiple languages. We then match against these fields, preferring matches which are analysed in english. The highest scoring of those `multi_match`es is added to the total document score. <https://github.com/wellcomecollection/catalogue-pipeline/pull/1654>

```json
{
  "dis_max": {
    "queries": [
      {
        "multi_match": {
          "query": "{{query}}",
          "fields": [
            "search.titlesAndContributors^100.0",
            "search.titlesAndContributors.english^100.0",
            "search.titlesAndContributors.shingles^100.0"
          ],
          "type": "best_fields",
          "operator": "And",
          "_name": "title and contributor exact spellings"
        }
      },
      {
        "multi_match": {
          "query": "{{query}}",
          "fields": [
            "search.titlesAndContributors.arabic",
            "search.titlesAndContributors.bengali",
            "search.titlesAndContributors.french",
            "search.titlesAndContributors.german",
            "search.titlesAndContributors.hindi",
            "search.titlesAndContributors.italian"
          ],
          "type": "best_fields",
          "operator": "And",
          "_name": "non-english titles and contributors"
        }
      }
    ]
  }
}
```

### Users should see documents whose titles are most obviously connected to their query at the top of the list

We use a `span_first` query to match the first tokens in the title field. The order of those terms is taken into account by using the `title.shingles` subfield. These matches are heavily boosted to ensure that the most obviously matched titles appear first.

For example, e.g. if we had three works

> Human genetic information : science, law, and ethics  
> International journal of law and information technology  
> Information law : compliance for librarians and information professionals  

and somebody searches for "Information law", all other things being equal,
we want to prioritise the third result. Based on user feedback documented here <https://github.com/wellcomecollection/catalogue-api/issues/466>

```json
{
  "span_first": {
    "match": {
      "span_term": {
        "data.title.shingles": "{{query}}"
      }
    },
    "end": 1,
    "boost": 1000
  }
}
```
