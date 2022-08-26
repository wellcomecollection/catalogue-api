# Rank cluster

Our experiments can (and do!) produce long-running queries which can negatively affect the performance of services which real users see.

To avoid affecting the production services, we run the rank tests against a _copy_ of the data in a completely separate cluster, copied using [elasticsearch's CCR feature](https://www.elastic.co/guide/en/elasticsearch/reference/current/xpack-ccr.html).

CCR is a memory intensive operation for both the leader and follower clusters - so much so that CCR itself can induce serious performance problems in the production cluster which can be tricky to resolve. To avoid that scenario, use the following procedure for refreshing indices in the rank cluster.

## Replicating production indices to rank

1. Make sure you have enough time to do this whole process, and let others know what's happening in case it goes wrong.
2. Scale up the production pipeline ES cluster, either using `scale_up_elastic_cluster` in the [pipeline Terraform module](https://github.com/wellcomecollection/catalogue-pipeline/blob/main/pipeline/terraform/main.tf), or by setting it manually to the 58gb configuration in Elastic Cloud.
3. Replicate the works index:
   1. Open Kibana for the rank cluster
   2. Open "Cross Cluster Replication" under "Stack Management"
   3. Add the pipeline cluster as a new remote cluster, using the endpoint URL from the Elastic Cloud "Overview" page for the pipeline deployment.
   4. Set the leader and follower indices to `works-indexed-${pipeline date}`, and create the follower index.
   5. Replication has now begun - even if Kibana is showing that it hasn't! If you wait a while and look in "Index Management", you should see the new index fill up.
   6. Once complete, pause replication for the follower index. You can do this even when the index is still yellow (that just means that not every shard is replicated, and finishing that process does not require CCR).
4. Repeat step 3 for the images index: `images-indexed-${pipeline date}`.
5. Scale the production pipeline ES cluster back down (8gb).

_If something goes wrong and the production cluster is overwhelmed, pausing the replication and applying a rolling restart to the production cluster should make things right again._
