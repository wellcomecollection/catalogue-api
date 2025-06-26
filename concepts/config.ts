import { URL } from "url";
import { z } from "zod";

const environmentSchema = z.object({
  PUBLIC_ROOT_URL: z
    .string()
    .url()
    .default("https://api.wellcomecollection.org/catalogue/v2"),
});
const environment = environmentSchema.parse(process.env);

const config = {
  pipelineDate: "2025-05-01",
  conceptsIndex: "concepts-indexed-2025-06-17",
  publicRootUrl: new URL(environment.PUBLIC_ROOT_URL),
};

export type Config = typeof config;

export const getConfig = () => config;
