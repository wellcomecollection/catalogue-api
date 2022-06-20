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
  pipelineDate: "2022-06-18",
  serviceName: "catalogue_api", // TODO provision an elasticsearch user for the concepts API
  publicRootUrl: new URL(environment.PUBLIC_ROOT_URL),
} as const;

export type Config = typeof config;

export const getConfig = () => config;