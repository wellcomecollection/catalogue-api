# Query releases

## 23 April 20201
### Images query

Brings the images query inline with the learning around languages etc 
from the works query.

[PR](https://github.com/wellcomecollection/rank/pull/35)

## 7 April
## Language analyzers

Adds [language analyzers](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-lang-analyzer.html)
to be able to handle conjugation, compounding etc.

[PR](https://github.com/wellcomecollection/rank/pull/31)

---

## 25 March 2021
### Alternative spellings

Creates a new part of the query matching with `AUTO` fuzziness.

This allows for matching of alternative spellings in the way we transliterate.

e.g.
* "Arkaprakasa", "Arkaprakāśa", "Arka-prakasha", "Arka prakāśa".
* nuğūm, nujūm

We boost this slightly lower to score exact matches more highly.
This helps with things like "swimming" vs "stimming".


[Query PR](https://github.com/wellcomecollection/rank/pull/22)

[Example PR](https://github.com/wellcomecollection/rank/pull/23)