import { URL } from "url";
import { z } from "zod";

const environmentSchema = z.object({
  PUBLIC_ROOT_URL: z
    .string()
    .url()
    .default("https://api.wellcomecollection.org/catalogue/v2"),
});
const environment = environmentSchema.parse(process.env);

export type ClusterConfig = {
  pipelineDate: string;
  conceptsIndex: string;
};

// Clusters a request can select with ?elasticCluster=<name>
const additionalClusters: Record<string, ClusterConfig> = {
  "pipeline-2026-09-30": {
    pipelineDate: "2026-09-30",
    conceptsIndex: "concepts-indexed-2026-09-30",
  },
};

const config = {
  pipelineDate: "2026-09-30",
  conceptsIndex: "concepts-indexed-2026-09-30",
  additionalClusters,
  publicRootUrl: new URL(environment.PUBLIC_ROOT_URL),
};

export type Config = typeof config;

export const getConfig = () => config;
