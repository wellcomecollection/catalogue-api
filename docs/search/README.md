# Search

These docs describe the development of our search engine and the systems we've used to test it.

## The story so far

We began with a [data-informed approach](./data_informed) to measuring relevance, running candidate queries against each other in AB tests, and taking incremental steps based on some illustrative metrics. After a few months of development, we found that this approach was insufficient to drive the development of new features safely or reliably. Most of this documentation is now outdated as a result.

Instead, we started developing [rank](./rank/), a system which uses known examples of tricky search-term / document pairs to constrain the query. Rank is the system we use to develop queries and mappings today.

## Current queries

You can read an explanation of the current queries' structure [here](./current/README.md).
