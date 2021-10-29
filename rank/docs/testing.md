# Testing

## Performance

Rank allows us to test the performance (precision, recall, etc) of new candidate queries and mappings, but real-world performance is based on more than just delivering trustworthy results. We also test the speed at which a query runs to make sure that each iteration is faster than the last.

We use real-world search terms from our own analytics systems to assess the performance of our queries. To fetch a set of search terms for testing, run:

```
yarn getSearchTerms
```

To compare the speed of candidate/production queries using those search terms, run:

```
yarn compareQuerySpeed
```
