# Rank cluster

Our experiments can (and do!) produce long-running queries which can negatively affect the performance of services which real users see.

To avoid affecting the production services, we run the rank tests against a _copy_ of the data in a completely separate cluster, copied using [elasticsearch's CCR feature](https://www.elastic.co/guide/en/elasticsearch/reference/current/xpack-ccr.html).

CCR is a memory intensive operation for both the leader and follower clusters - so much so that CCR itself can induce serious performance problems in the production cluster which can be tricky to resolve. To avoid that scenario, you should remember to:

- Drop a message in slack to let other people know you're going to replicate an index in the production cluster.
- Increase the size of the production cluster while the operation is running.
- Avoid replicating works and images indices at the same time.

When you're done, remember to scale the production cluster back down to its original size and update people in slack again.

If something goes wrong and the production cluster is overwhelmed, pausing the replication and applying a rolling restart to the production cluster should make things right again.