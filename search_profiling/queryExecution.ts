import dotenv from "dotenv";
import { Client } from "@elastic/elasticsearch";

dotenv.config();

const es = new Client({
  cloud: {
    id: process.env.ES_CLOUD_ID!,
  },
  auth: {
    username: process.env.ES_USER!,
    password: process.env.ES_PASS!,
  },
});

const constructQueryBody = (
  query: string,
  searchTerm: string,
  aggs?: string
): Record<string, any> => ({
  query: JSON.parse(query.replace(/{{query}}/g, searchTerm)),
  aggs: aggs ? JSON.parse(aggs) : undefined,
  from: 0,
  size: 10,
  sort: [
    {
      "state.canonicalId": {
        order: "asc",
      },
    },
  ],
  track_total_hits: true,
});

export const measureQueryTime = async ({
  index,
  query,
  searchTerm,
  aggs,
}: {
  index: string;
  query: string;
  searchTerm: string;
  aggs?: string;
}) => {
  const body = constructQueryBody(query, searchTerm, aggs);
  const result = await es.search({
    index,
    body,
  });
  return parseInt(result.body.took, 10);
};
