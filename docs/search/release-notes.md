# Query releases

## Less fuzz, more archives
### 17 June 2021

**Less fuzz**

Using `prefix_length: 2` - We only start fuzzy past the first two charaters. e.g. `Carrot` will not match `parrot`, but will match `Cardot`.

There is still quite a lot of work to do with fuzziness - but from the examples we had been given, and multiple tests against those, this covered most bases demonstrating the importance of the feedback and examples we get.

**Archive search** 

Combining archives AltRefNo and titles in search. The issue here is that we store AltRefNos as `keyword` types and titles as `text` types. This allows for exact matching on IDs, and free text search on titles. Free text searching unfortuinately strips punctuation, and thus `/` meaning that if I searched for `wa/hmm durham` it expanded to `wa AND hmm AND durham`.

We now store the titles and AltRefNos of works, and their relations in a field that stores both the exact ID and free text title, with the same query matching on `wa/hmm AND durham`.

**Titles and contributors**

One benefit of introducing fuzzy search was allowing for spellings people *might* use. And example we had was when searching for `Casils Time Lapse` we would expect to match `Time lapse by Cassils`. Searching across fields with fuzziness is not possible in elasticsearch, so we created another search field that combines the contect 

**What we lost**

While developing this query we hit quite a major performance snag. While we're still investigating, a way around it was to remove a part of how we were exact title matching. This meant that when searching for `The piggle` anything starting with `The piggle` was scored higher that just matching those terms. We removed this, so now `Preface to The Piggle` scores higher than `The Piggle : an account of the psychoanalytic treatment of a little girl / by D. W. Winnicott ; edited by Ishak Ramzy.` - but this still comes a close second. This was a comprimise that we were will to make for the tmie being, proving to us once again that search is full of compromise and tensions from many angles.

---

## Images query
### 23 April 2021

Brings the images query inline with the learning around languages etc 
from the works query.

[PR](https://github.com/wellcomecollection/rank/pull/35)

## Language analyzers
### 7 April

Adds [language analyzers](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-lang-analyzer.html)
to be able to handle conjugation, compounding etc.

[PR](https://github.com/wellcomecollection/rank/pull/31)

---

## Alternative spellings
### 25 March 2021


Creates a new part of the query matching with `AUTO` fuzziness.

This allows for matching of alternative spellings in the way we transliterate.

e.g.
* "Arkaprakasa", "Arkaprakāśa", "Arka-prakasha", "Arka prakāśa".
* nuğūm, nujūm

We boost this slightly lower to score exact matches more highly.
This helps with things like "swimming" vs "stimming".


[Query PR](https://github.com/wellcomecollection/rank/pull/22)

[Example PR](https://github.com/wellcomecollection/rank/pull/23)