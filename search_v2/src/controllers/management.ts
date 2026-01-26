import { RequestHandler } from "express";
import asyncHandler from "express-async-handler";
import { Clients } from "../types";
import { Config } from "../../config";

// ============================================================================
// Cluster Health Controller (GET /management/clusterhealth)
// ============================================================================

export const clusterHealthController = (clients: Clients): RequestHandler => {
  const elasticClient = clients.elastic;

  return asyncHandler(async (_req, res) => {
    const health = await elasticClient.cluster.health();
    res.status(200).json(health);
  });
};

// ============================================================================
// Work Types Tally Controller (GET /management/_workTypes)
// ============================================================================

export const workTypesTallyController = (
  clients: Clients,
  config: Config
): RequestHandler => {
  const elasticClient = clients.elastic;

  return asyncHandler(async (_req, res) => {
    const response = await elasticClient.search({
      index: config.worksIndex,
      body: {
        size: 0,
        aggs: {
          workTypes: {
            terms: {
              field: "type",
              size: 10,
            },
          },
        },
      },
    });

    const buckets = (response.aggregations?.workTypes as any)?.buckets ?? [];
    const result: Record<string, number> = {};

    for (const bucket of buckets) {
      result[bucket.key] = bucket.doc_count;
    }

    res.status(200).json(result);
  });
};

// ============================================================================
// Search Templates Controller (GET /_searchTemplates)
// ============================================================================

export const searchTemplatesController = (config: Config): RequestHandler => {
  return asyncHandler(async (_req, res) => {
    res.status(200).json({
      templates: {
        works: "WorksQuery.json",
        images: "ImagesQuery.json",
      },
    });
  });
};

// ============================================================================
// Elastic Config Controller (GET /_elasticConfig)
// ============================================================================

export const elasticConfigController = (config: Config): RequestHandler => {
  return asyncHandler(async (_req, res) => {
    res.status(200).json({
      worksIndex: config.worksIndex,
      imagesIndex: config.imagesIndex,
      pipelineDate: config.pipelineDate,
    });
  });
};
