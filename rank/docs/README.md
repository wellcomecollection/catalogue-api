# Docs

Rank's built with:

- [next.js](https://nextjs.org/) and [typescript](https://www.typescriptlang.org/) for structuring the content
- [vercel](https://vercel.com/docs/cli) and the `main` branch of this repo for deployment
- [tailwind css](https://tailwindcss.com/) for styling the content
- [elasticsearch](https://www.elastic.co/) for storing the catalogue data and running queries
- [elasticsearch's rank_eval API](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html) for measuring performance on known results

A rough diagram of how rank and the other weco services talk to one another:

![diagram](diagram.png)

Rank doesn't run queries against production data. Our experiments can (and do!) produce long-running queries, which can negatively affect the performance of services which real users see.

Instead, we run our tests and experimental queries against [a replica of the live data in a separate cluster](https://www.elastic.co/guide/en/elasticsearch/reference/current/xpack-ccr.html), which is repopulated every night.
