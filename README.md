# catalogue-api

[![Build status](https://badge.buildkite.com/1d9006a0f151dd00522ff3ed59a247997016288b6b7ba30efa.svg?branch=main)](https://buildkite.com/wellcomecollection/catalogue-api)
[![Deploy stage](https://img.shields.io/buildkite/41057eb74a0e2c22d2f78c325bfa6b90458b9529b2bb532b85/main.svg?label=deploy%20stage)](https://buildkite.com/wellcomecollection/catalogue-api-deploy-stage)
[![Deploy prod](https://img.shields.io/buildkite/b7212f6ddcd97f0888e7e7c9064648100a66b54df7e00d5f97/main.svg?label=deploy%20prod)](https://buildkite.com/wellcomecollection/catalogue-api-deploy-prod)

The Catalogue API provides a unified, consistent interface for working with our museum and library collections.
It combines our different source catalogues and presents them through a single set of APIs.

![Left: a collection of icons representing a database, folder, scroll, labelled "Source catalogues". Right: a green circle labelled "Catalogue API". There's an arrow flowing from left to right.](illustration.svg)

## Overview

We have two services for searching the museum and library collections.
These APIs are freely available, and allow anybody to use our data.

-	The **search API** is for running ad hoc searches, and it powers the collections search at [wellcomecollection.org/collections][search].
	We have [documentation][search_docs] for external developers who want to use this API.
	
	To help us develop the search API, we have a tool called **rank**.
	This helps us measure the quality of our search ranking, by checking that certain queries return known-relevant results.

-	The **catalogue datasets** provide a daily snapshot of all the works in the search API.
	It's useful for batch queries or analysis that doesn't work with the search API.
	These snapshots are [freely available to download][snapshots].

Both of these services read the data populated by the [catalogue pipeline][pipeline].

We also have two services for dealing with items in the library stores.
These APIs are used on the Wellcome Collection website, but they require authentication and aren't publicly available:

-	The **items API** gets the status of an item: for example, whether it's on hold, or available for requesting, or temporarily unavailable.
	It queries source systems directly, so it always has the most up-to-date information.

-	Library members can [request items from the library stores][requests].
	The **requests API** allows them to manage their requests on the Wellcome Collection website: either placing requests, or checking the status of their outstanding requests.
	It forward requests to our library management systems, so that library staff know which items to retrieve from the stores.

[search]: https://wellcomecollection.org/collections
[search_docs]: https://developers.wellcomecollection.org/catalogue
[pipeline]: https://github.com/wellcomecollection/catalogue-pipeline
[snapshots]: https://developers.wellcomecollection.org/datasets
[requests]: https://wellcomecollection.org/pages/X_2eexEAACQAZLBi

## Usage

Anybody can use the search API or the snapshots.
Both are freely accessible over HTTP, with no authentication or sign-up required.

For example, you can get a list of works:

```
curl "https://api.wellcomecollection.org/catalogue/v2/works"
```

Or you could search for particular words:

```
curl "https://api.wellcomecollection.org/catalogue/v2/works?query=cat"
```

Or you could search for works from a particular period:

```
curl "https://api.wellcomecollection.org/catalogue/v2/works?production.dates.from=1900-01-01&production.dates.to=1999-12-31"
```

Our [developer documentation][developer_docs] explains how to use the search API in more detail, and where to download our catalogue datasets.

The items and requests APIs are only for use by other Wellcome Collection services.

[developer_docs]: https://developers.wellcomecollection.org

## Development

See [docs/developers.md](docs/developers.md).

## License

MIT.
