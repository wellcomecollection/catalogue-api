import { Config } from "../../config";
import { ResilientElasticClient } from "../services/elasticsearch";
import { Clients } from "../types";
import { HttpError } from "./error";

type Cluster = { elastic: ResilientElasticClient; index: string };

// Mirrors the search API: no param means the default cluster, an unknown name is a 404
export const clusterGetter =
  (clients: Clients, config: Config) =>
  (name: string | undefined): Cluster => {
    if (name === undefined) {
      return { elastic: clients.elastic, index: config.conceptsIndex };
    }
    if (
      !Object.hasOwn(clients.additionalElastic, name) ||
      !Object.hasOwn(config.additionalClusters, name)
    ) {
      throw new HttpError({
        status: 404,
        label: "Not Found",
        description: `Cluster '${name}' is not configured`,
      });
    }
    return {
      elastic: clients.additionalElastic[name],
      index: config.additionalClusters[name].conceptsIndex,
    };
  };
